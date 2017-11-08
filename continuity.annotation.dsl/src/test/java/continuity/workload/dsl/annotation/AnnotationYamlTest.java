package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationYamlTest {

	private static SystemAnnotation annotation;

	@BeforeClass
	public static void setupAnnotation() {
		annotation = ContinuityModelTestInstance.SIMPLE.getAnnotation();
	}

	@Test
	public void test() throws JsonGenerationException, JsonMappingException, IOException {
		ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);
		serializer.writeToYaml(annotation, "annotation.yml");
		SystemAnnotation read = serializer.readFromYaml("annotation.yml");
		System.out.println(read);
	}

}
