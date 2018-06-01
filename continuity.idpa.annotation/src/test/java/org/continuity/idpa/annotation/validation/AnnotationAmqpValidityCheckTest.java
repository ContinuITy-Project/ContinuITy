package org.continuity.idpa.annotation.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.entities.report.ModelElementReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.amqp.AnnotationAmpqHandler;
import org.continuity.idpa.annotation.storage.AnnotationStorage;
import org.continuity.idpa.annotation.storage.AnnotationStorageManager;
import org.continuity.idpa.application.Application;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationAmqpValidityCheckTest {

	private static final String TAG = "RabbitAnnotationValidityTest";

	private static final int NOTHING_BEFORE_FIRST = 1;
	private static final int THIRD_BEFORE_FIRST = 2;
	private static final int FIRST_BEFORE_FIRST = 3;

	private static final int FIRST_BEFORE_SECOND = 4;
	private static final int SECOND_BEFORE_SECOND = 5;

	private AnnotationAmpqHandler annotationHandler;
	private AnnotationStorageManager storageManager;

	private RestTemplate restMock;

	private AmqpTemplate amqpMock;

	private int firstMode;
	private int secondMode;

	@Before
	public void setup() throws IOException {
		Path tempDir = Files.createTempDirectory("AnnotationAmqpValidityCheckTest");

		restMock = Mockito.mock(RestTemplate.class);

		for (AnnotationValidityTestInstance testInstance : AnnotationValidityTestInstance.values()) {
			Mockito.when(restMock.getForEntity(testInstance.getSystemLink(), Application.class)).thenReturn(testInstance.getSystemEntity());
			Mockito.when(restMock.getForEntity(testInstance.getAnnotationLink(), ApplicationAnnotation.class)).thenReturn(testInstance.getAnnotationEntity());
		}

		AnnotationValidityReportBuilder builder = new AnnotationValidityReportBuilder();
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_REMOVED, new ModelElementReference("HttpInterface", "logout")));
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_ADDED, new ModelElementReference("HttpInterface", "login")));
		AnnotationValidityReport firstReport = builder.buildReport();

		builder = new AnnotationValidityReportBuilder();
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_ADDED, new ModelElementReference("HttpInterface", "logout")));
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.PARAMETER_ADDED, new ModelElementReference("HttpParameter", "logoutuser")));
		AnnotationValidityReport secondReport = builder.buildReport();

		builder = new AnnotationValidityReportBuilder();
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_REMOVED, new ModelElementReference("HttpInterface", "login")));
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.PARAMETER_REMOVED, new ModelElementReference("HttpParameter", "logoutuser")));
		AnnotationValidityReport thirdReport = builder.buildReport();

		builder = new AnnotationValidityReportBuilder();
		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_ADDED, new ModelElementReference("HttpInterface", "login")));
		AnnotationValidityReport allFromFirstReport = builder.buildReport();

		AnnotationValidityReport emptyReport = new AnnotationValidityReport(Collections.emptySet(), Collections.emptyMap());

		firstMode = NOTHING_BEFORE_FIRST;
		secondMode = FIRST_BEFORE_SECOND;

		Mockito.when(restMock.getForEntity(AnnotationValidityTestInstance.FIRST.getSystemLink() + "/delta", AnnotationValidityReport.class))
		.thenAnswer(invoc -> {
			switch (firstMode) {
			case NOTHING_BEFORE_FIRST:
				return ResponseEntity.ok(allFromFirstReport);
			case THIRD_BEFORE_FIRST:
				return ResponseEntity.ok(firstReport);
			case FIRST_BEFORE_FIRST:
				return ResponseEntity.ok(emptyReport);
			default:
				return ResponseEntity.ok();
			}
		});
		Mockito.when(restMock.getForEntity(AnnotationValidityTestInstance.SECOND_SYSTEM.getSystemLink() + "/delta", AnnotationValidityReport.class)).thenAnswer(invoc -> {
			switch (secondMode) {
			case FIRST_BEFORE_SECOND:
				return ResponseEntity.ok(secondReport);
			case SECOND_BEFORE_SECOND:
				return ResponseEntity.ok(emptyReport);
			default:
				return ResponseEntity.ok();
			}
		});
		Mockito.when(restMock.getForEntity(AnnotationValidityTestInstance.THIRD_SYSTEM.getSystemLink() + "/delta", AnnotationValidityReport.class)).thenReturn(ResponseEntity.ok(thirdReport));
		Mockito.when(restMock.getForEntity(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION.getSystemLink() + "/delta", AnnotationValidityReport.class)).thenReturn(ResponseEntity.ok(secondReport));
		Mockito.when(restMock.getForEntity(AnnotationValidityTestInstance.SECOND_ANNOTATION.getSystemLink() + "/delta", AnnotationValidityReport.class)).thenReturn(ResponseEntity.ok(emptyReport));
		Mockito.when(restMock.getForEntity(AnnotationValidityTestInstance.THIRD_ANNOTATION.getSystemLink() + "/delta", AnnotationValidityReport.class)).thenReturn(ResponseEntity.ok(emptyReport));

		amqpMock = Mockito.mock(AmqpTemplate.class);

		storageManager = new AnnotationStorageManager(new AnnotationStorage(tempDir));
		annotationHandler = new AnnotationAmpqHandler(storageManager, restMock, amqpMock);
	}

	@Test
	public void testChangingSystemModels() throws IOException {
		firstMode = NOTHING_BEFORE_FIRST;
		// If the system model would be stored fist, there would be no check for validity. Actually,
		// this behavior is not correct. However, since we do not expect this to happen in practice
		// (except for the case that a new system model and annotation is extracted from a workload
		// model for the first time, but there will be no violations), we ignore this case here.
		callAnnotationCreated(AnnotationValidityTestInstance.FIRST);
		callSystemModelCreated(AnnotationValidityTestInstance.FIRST);
		ArgumentCaptor<AnnotationValidityReport> reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		AnnotationValidityReport report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertTrue(report.getViolations().isEmpty());

		Mockito.reset(amqpMock);

		callSystemModelCreated(AnnotationValidityTestInstance.SECOND_SYSTEM);
		reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());

		Mockito.reset(amqpMock);

		callSystemModelCreated(AnnotationValidityTestInstance.THIRD_SYSTEM);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());

		Mockito.reset(amqpMock);

		firstMode = THIRD_BEFORE_FIRST;
		callSystemModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertTrue(storageManager.getAnnotation(TAG).getEndpointAnnotations().isEmpty());

		Mockito.reset(amqpMock);

		firstMode = FIRST_BEFORE_FIRST;
		callSystemModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verifyZeroInteractions(amqpMock);
		Assert.assertTrue(storageManager.getAnnotation(TAG).getEndpointAnnotations().isEmpty());

		// Reset annotation (has been fixed until it was empty)
		storageManager.updateAnnotation(TAG, AnnotationValidityTestInstance.FIRST.getAnnotation());
		Mockito.verifyZeroInteractions(amqpMock);
		Assert.assertFalse(storageManager.getAnnotation(TAG).getEndpointAnnotations().isEmpty());
	}

	@Test
	public void testChangingSystemModelsWithUltimateAnnotation() {
		callAnnotationCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		callSystemModelCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		ArgumentCaptor<AnnotationValidityReport> reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		AnnotationValidityReport report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());

		Mockito.reset(amqpMock);

		secondMode = SECOND_BEFORE_SECOND;
		callAnnotationCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		callSystemModelCreated(AnnotationValidityTestInstance.SECOND_SYSTEM);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callAnnotationCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		callSystemModelCreated(AnnotationValidityTestInstance.THIRD_SYSTEM);
		reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertTrue(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());

		Mockito.reset(amqpMock);

		firstMode = THIRD_BEFORE_FIRST;
		callAnnotationCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		callSystemModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());

		// Do it twice, since now the "oldSystemModel" is the same
		Mockito.reset(amqpMock);

		firstMode = FIRST_BEFORE_FIRST;
		callAnnotationCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		callSystemModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verifyZeroInteractions(amqpMock);
	}

	/**
	 * Since only the first annotation is kept and not overwritten, no violations should be
	 * reported.
	 */
	@Test
	public void testChangingAnnotations() {
		firstMode = NOTHING_BEFORE_FIRST;
		callSystemModelCreated(AnnotationValidityTestInstance.FIRST);
		ArgumentCaptor<AnnotationValidityReport> reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.name()), ArgumentMatchers.eq("report"), reportCaptor.capture());
		AnnotationValidityReport report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertTrue(report.getViolations().isEmpty());

		Mockito.reset(amqpMock);

		callAnnotationCreated(AnnotationValidityTestInstance.SECOND_ANNOTATION);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callAnnotationCreated(AnnotationValidityTestInstance.THIRD_ANNOTATION);
		Mockito.verifyZeroInteractions(amqpMock);
	}

	private void callSystemModelCreated(AnnotationValidityTestInstance testInstance) {
		LinkExchangeModel link = new LinkExchangeModel();
		link.setTag(TAG);

		link.setApplicationLink(testInstance.getSystemLink());
		link.setInitialAnnotationLink(testInstance.getAnnotationLink());
		link.setDeltaLink(testInstance.getSystemLink() + "/delta");

		annotationHandler.onApplicationModelChanged(link);
	}

	private void callAnnotationCreated(AnnotationValidityTestInstance testInstance) {
		LinkExchangeModel link = new LinkExchangeModel();
		link.setTag(TAG);

		link.setApplicationLink(testInstance.getSystemLink());
		link.setInitialAnnotationLink(testInstance.getAnnotationLink());
		link.setDeltaLink(testInstance.getSystemLink() + "/delta");

		annotationHandler.onAnnotationModelCreated(link);
	}

}
