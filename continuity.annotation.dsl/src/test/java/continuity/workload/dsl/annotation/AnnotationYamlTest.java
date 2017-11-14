package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationYamlTest {

	@Test
	public void testAnnotation() throws JsonGenerationException, JsonMappingException, IOException {
		testAnnotationWith(ContinuityModelTestInstance.SIMPLE.getAnnotation());
		testAnnotationWith(ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation());
	}

	private void testAnnotationWith(SystemAnnotation annotation) throws JsonGenerationException, JsonMappingException, IOException {
		ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);
		String yaml = serializer.writeToYamlString(annotation);
		SystemAnnotation read = serializer.readFromYamlString(yaml);
		String yaml2 = serializer.writeToYamlString(read);

		Assert.assertEquals("Re-serialized yaml should be equal to original one!", yaml, yaml2);
	}

	@Test
	public void testSystem() throws JsonGenerationException, JsonMappingException, IOException {
		testSystemWith(ContinuityModelTestInstance.SIMPLE.getSystemModel());
		testSystemWith(ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel());
	}

	private void testSystemWith(SystemModel systemModel) throws JsonGenerationException, JsonMappingException, IOException {
		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);
		String yaml = serializer.writeToYamlString(systemModel);
		SystemModel read = serializer.readFromYamlString(yaml);
		String yaml2 = serializer.writeToYamlString(read);

		Assert.assertEquals("Re-serialized yaml should be equal to original one!", yaml, yaml2);
	}

}
