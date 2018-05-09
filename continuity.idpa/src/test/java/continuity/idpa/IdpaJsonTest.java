package continuity.idpa;

import java.io.IOException;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.test.IdpaTestInstance;
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
public class IdpaJsonTest {

	private ObjectMapper mapper = new ObjectMapper(new JsonFactory());

	@Test
	public void testAnnotation() throws JsonGenerationException, JsonMappingException, IOException {
		String json = mapper.writeValueAsString(IdpaTestInstance.DVDSTORE_PARSED.getAnnotation());
		ApplicationAnnotation parsed = mapper.readValue(json, ApplicationAnnotation.class);
		String json2 = mapper.writeValueAsString(parsed);

		Assert.assertEquals("Re-serialized json should be equal to original one!", json, json2);
	}

	@Test
	public void testSystem() throws JsonGenerationException, JsonMappingException, IOException {
		String json = mapper.writeValueAsString(IdpaTestInstance.DVDSTORE_PARSED.getApplication());
		Application parsed = mapper.readValue(json, Application.class);
		String json2 = mapper.writeValueAsString(parsed);

		Assert.assertEquals("Re-serialized json should be equal to original one!", json, json2);
	}

}
