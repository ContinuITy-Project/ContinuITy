package continuity.idpa.annotation.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.json.JsonDerivedValue;
import org.continuity.idpa.annotation.json.JsonInput;
import org.continuity.idpa.annotation.json.JsonObject;
import org.continuity.idpa.serialization.json.IdpaSerializationUtils;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonInputTest {

	private IdpaYamlSerializer<ApplicationAnnotation> serializer;

	private ObjectMapper mapper;

	private ApplicationAnnotation annotation;

	@Before
	public void setup() {
		this.serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

		this.mapper = IdpaSerializationUtils.getDefaultJsonObjectMapper();

		this.annotation = new ApplicationAnnotation();

		DirectListInput listInput = new DirectListInput();
		listInput.setId("Input_list");
		listInput.setData(Collections.singletonList("foo"));

		JsonInput input = new JsonInput();
		input.setId("Input_json");
		JsonObject obj = new JsonObject();
		input.setJson(obj);
		JsonDerivedValue derivedValue = new JsonDerivedValue();
		derivedValue.setInput(listInput);
		obj.setItems(new HashMap<>());
		obj.getItems().put("derived", derivedValue);

		annotation.addInput(listInput);
		annotation.addInput(input);
	}

	@Test
	public void testReadWriteYaml() throws JsonParseException, JsonMappingException, IOException {
		// DON'T TOUCH THE FILE!!!
		String yaml = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/annotation-json.yml"))).lines().collect(Collectors.joining("\n"));
		ApplicationAnnotation ann = serializer.readFromYamlInputStream(getClass().getResourceAsStream("/annotation-json.yml"));

		String newYaml = serializer.writeToYamlString(ann);

		assertThat(newYaml).isEqualTo(yaml);
	}

	@Test
	public void testWriteReadYaml() throws JsonParseException, JsonMappingException, IOException {
		String yaml = serializer.writeToYamlString(annotation);
		ApplicationAnnotation parsed = serializer.readFromYamlString(yaml);

		assertThat(parsed.getInputs()).isEqualTo(parsed.getInputs());
	}

	@Test
	public void testWriteReadJson() throws JsonParseException, JsonMappingException, IOException {
		String json = mapper.writeValueAsString(annotation);
		ApplicationAnnotation parsed = mapper.readValue(json, ApplicationAnnotation.class);

		assertThat(parsed.getInputs()).isEqualTo(parsed.getInputs());
	}

}
