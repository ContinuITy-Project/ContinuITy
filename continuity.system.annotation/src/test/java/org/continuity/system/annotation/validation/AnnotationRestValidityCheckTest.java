package org.continuity.system.annotation.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.continuity.system.annotation.controllers.AnnotationController;
import org.continuity.system.annotation.entities.AnnotationValidityReport;
import org.continuity.system.annotation.entities.ModelElementReference;
import org.continuity.system.annotation.storage.AnnotationStorage;
import org.continuity.system.annotation.storage.AnnotationStorageManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationRestValidityCheckTest {

	private static final String TAG = "AnnotationRestValidityCheckTest";

	private AnnotationController controller;

	private AnnotationStorage storageMock;

	private ObjectMapper mapper;

	@Before
	public void setupController() throws IOException {
		storageMock = Mockito.mock(AnnotationStorage.class);
		Mockito.when(storageMock.readSystemModel(TAG)).thenReturn(AnnotationValidityTestInstance.FIRST.getSystemModel());

		controller = new AnnotationController(new AnnotationStorageManager(storageMock));
	}

	@Before
	public void setupJsonMapper() {
		mapper = new ObjectMapper();
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addKeyDeserializer(ModelElementReference.class, new ModelElementReferenceKeyDeserializer());
		mapper.registerModule(simpleModule);
	}

	@Test
	public void testChangingAnnotations() throws IOException {
		ResponseEntity<String> response = controller.updateAnnotation(TAG, AnnotationValidityTestInstance.FIRST.getAnnotation());
		Mockito.verify(storageMock).readSystemModel(ArgumentMatchers.eq(TAG));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		Mockito.clearInvocations(storageMock);

		response = controller.updateAnnotation(TAG, AnnotationValidityTestInstance.SECOND_ANNOTATION.getAnnotation());
		Mockito.verify(storageMock).readSystemModel(ArgumentMatchers.eq(TAG));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		Mockito.clearInvocations(storageMock);

		response = controller.updateAnnotation(TAG, AnnotationValidityTestInstance.THIRD_ANNOTATION.getAnnotation());
		Mockito.verify(storageMock).readSystemModel(ArgumentMatchers.eq(TAG));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		AnnotationValidityReport report = mapper.readValue(response.getBody(), AnnotationValidityReport.class);
		Assert.assertFalse(report.isOk());
		Assert.assertTrue(report.isBreaking());
	}

	private static class ModelElementReferenceKeyDeserializer extends KeyDeserializer {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
			String[] tokens = key.split("\\[");

			return new ModelElementReference(tokens[1].substring(0, tokens[1].length() - 1), tokens[0].trim());
		}

	}

}
