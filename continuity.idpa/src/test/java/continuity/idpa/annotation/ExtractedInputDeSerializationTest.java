package continuity.idpa.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.serialization.IdpaSerializationUtils;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExtractedInputDeSerializationTest {

	private IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

	private ObjectMapper jsonMapper = IdpaSerializationUtils.getDefaultJsonObjectMapper();

	@Test
	public void testReadWriteYaml() throws JsonParseException, JsonMappingException, IOException {
		String yaml = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/annotation-extraction-input.yml"))).lines().collect(Collectors.joining("\n"));
		ApplicationAnnotation ann = serializer.readFromYamlString(yaml);

		String newYaml = serializer.writeToYamlString(ann);

		assertThat(newYaml).isEqualTo(yaml);
	}

	public void testWriteReadJson() throws JsonParseException, JsonMappingException, IOException {
		String yaml = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/annotation-extraction-input.yml"))).lines().collect(Collectors.joining("\n"));
		ApplicationAnnotation ann = serializer.readFromYamlString(yaml);

		String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ann);
		ApplicationAnnotation parsed = jsonMapper.readValue(json, ApplicationAnnotation.class);
		String newYaml = serializer.writeToYamlString(parsed);

		assertThat(newYaml).isEqualTo(yaml);
	}

}
