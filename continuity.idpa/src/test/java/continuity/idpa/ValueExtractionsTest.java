package continuity.idpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.JsonPathExtraction;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ValueExtractionsTest {

	private ObjectMapper mapper;

	private IdpaYamlSerializer<ApplicationAnnotation> serializer;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
		serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
	}

	@Test
	public void test() throws JsonParseException, JsonMappingException, IOException {
		ApplicationAnnotation ann = serializer.readFromYamlInputStream(getClass().getResourceAsStream("/annotation-extraction.yml"));
		check(ann, "deserialized from YAML");

		String json = mapper.writeValueAsString(ann);
		ann = mapper.readValue(json, ApplicationAnnotation.class);
		check(ann, "re-deserialized from JSON");
	}

	private void check(ApplicationAnnotation ann, String descr) {
		assertThat(ann.getInputs()).filteredOn(input -> input instanceof ExtractedInput).extracting(input -> (ExtractedInput) input).flatExtracting(ExtractedInput::getExtractions)
				.extracting(Object::getClass).extracting(Class::toGenericString)
				.as("The ann " + descr + " should contain exactly two extractions of type RegExExtraction and JsonPathExtraction")
				.containsExactlyInAnyOrder(RegExExtraction.class.toGenericString(), JsonPathExtraction.class.toGenericString());
	}

}
