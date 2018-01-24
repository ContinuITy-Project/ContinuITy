package org.continuity.system.annotation.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.system.annotation.amqp.AnnotationAmpqHandler;
import org.continuity.system.annotation.config.RabbitMqConfig;
import org.continuity.system.annotation.entities.AnnotationValidityReport;
import org.continuity.system.annotation.entities.SystemAnnotationLink;
import org.continuity.system.annotation.storage.AnnotationStorage;
import org.continuity.system.annotation.storage.AnnotationStorageManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationAmqpValidityCheckTest {

	private AnnotationAmpqHandler annotationHandler;

	private RestTemplate restMock;

	private AmqpTemplate amqpMock;

	@Before
	public void setupAnnotationHandler() throws IOException {
		Path tempDir = Files.createTempDirectory("TestTest");

		restMock = Mockito.mock(RestTemplate.class);

		for (AnnotationValidityTestInstance testInstance : AnnotationValidityTestInstance.values()) {
			Mockito.when(restMock.getForEntity(testInstance.getSystemLink(), SystemModel.class)).thenReturn(testInstance.getSystemEntity());
			Mockito.when(restMock.getForEntity(testInstance.getAnnotationLink(), SystemAnnotation.class)).thenReturn(testInstance.getAnnotationEntity());
		}

		amqpMock = Mockito.mock(AmqpTemplate.class);

		annotationHandler = new AnnotationAmpqHandler(new AnnotationStorageManager(new AnnotationStorage(tempDir)), restMock, amqpMock);
	}

	@Test
	public void testChangingSystemModels() {
		callModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.SECOND_SYSTEM);
		ArgumentCaptor<AnnotationValidityReport> reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME), ArgumentMatchers.eq("report"), reportCaptor.capture());
		AnnotationValidityReport report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.THIRD_SYSTEM);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verifyZeroInteractions(amqpMock);
	}

	@Test
	public void testChangingSystemModelsWithUltimateAnnotation() {
		callModelCreated(AnnotationValidityTestInstance.ULTIMATE_ANNOTATION);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.SECOND_SYSTEM);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.THIRD_SYSTEM);
		ArgumentCaptor<AnnotationValidityReport> reportCaptor = ArgumentCaptor.forClass(AnnotationValidityReport.class);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME), ArgumentMatchers.eq("report"), reportCaptor.capture());
		AnnotationValidityReport report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertTrue(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());

		// Do it twice, since now the "oldSystemModel" is the same
		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verify(amqpMock).convertAndSend(ArgumentMatchers.eq(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME), ArgumentMatchers.eq("report"), reportCaptor.capture());
		report = reportCaptor.getValue();
		Assert.assertFalse(report.isOk());
		Assert.assertFalse(report.isBreaking());
		Assert.assertNotNull(report.getViolationsBeforeFix());
		Assert.assertFalse(report.getViolationsBeforeFix().isEmpty());
	}

	/**
	 * Since only the first annotation is kept and not overwritten, no violations should be
	 * reported.
	 */
	@Test
	public void testChangingAnnotations() {
		callModelCreated(AnnotationValidityTestInstance.FIRST);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.SECOND_ANNOTATION);
		Mockito.verifyZeroInteractions(amqpMock);

		Mockito.reset(amqpMock);

		callModelCreated(AnnotationValidityTestInstance.THIRD_ANNOTATION);
		Mockito.verifyZeroInteractions(amqpMock);
	}

	private void callModelCreated(AnnotationValidityTestInstance testInstance) {
		SystemAnnotationLink link = new SystemAnnotationLink();
		link.setTag("RabbitAnnotationValidityTest");

		link.setSystemModelLink(testInstance.getSystemLink());
		link.setAnnotationLink(testInstance.getAnnotationLink());
		annotationHandler.onSystemModelCreated(link);
		annotationHandler.onAnnotationModelCreated(link);
	}

}
