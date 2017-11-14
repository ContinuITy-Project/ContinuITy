package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Henning Schulz
 *
 */
public class AnnotationJsonTest {

	private ObjectMapper mapper = new ObjectMapper(new JsonFactory());

	@Test
	public void testAnnotation() throws JsonGenerationException, JsonMappingException, IOException {
		String json = mapper.writeValueAsString(ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation());
		SystemAnnotation parsed = mapper.readValue(json, SystemAnnotation.class);
		String json2 = mapper.writeValueAsString(parsed);

		Assert.assertEquals("Re-serialized json should be equal to original one!", json, json2);
	}

	@Test
	public void testSystem() throws JsonGenerationException, JsonMappingException, IOException {
		String json = mapper.writeValueAsString(ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel());
		SystemModel parsed = mapper.readValue(json, SystemModel.class);
		String json2 = mapper.writeValueAsString(parsed);

		Assert.assertEquals("Re-serialized json should be equal to original one!", json, json2);
	}

}
