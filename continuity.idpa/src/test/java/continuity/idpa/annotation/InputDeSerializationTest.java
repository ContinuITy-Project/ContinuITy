package continuity.idpa.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CombinedInput;
import org.continuity.idpa.annotation.DatetimeInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.RandomNumberInput;
import org.continuity.idpa.annotation.RandomStringInput;
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

public class InputDeSerializationTest {

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
		listInput.setData(Collections.singletonList("42"));

		JsonInput jsonInput = new JsonInput();
		jsonInput.setId("Input_json");
		JsonObject obj = new JsonObject();
		jsonInput.setJson(obj);
		JsonDerivedValue derivedValue = new JsonDerivedValue();
		derivedValue.setInput(listInput);
		obj.setItems(new HashMap<>());
		obj.getItems().put("derived", derivedValue);

		RandomNumberInput randomNumberInput = new RandomNumberInput();
		randomNumberInput.setId("Input_random_number");
		randomNumberInput.setStaticLowerLimit(5);
		randomNumberInput.setDerivedUpperLimit(listInput);

		RandomStringInput randomStringInput = new RandomStringInput();
		randomStringInput.setId("Input_random_string");
		randomStringInput.setTemplate("[0-9A-D]{8}\\-[0-9A-D]{4}\\-[0-9A-D]{4}\\-[0-9A-D]{4}\\-[0-9A-D]{12}");

		DatetimeInput datetimeInput = new DatetimeInput();
		datetimeInput.setId("Input_datetime");
		datetimeInput.setFormat("yyyy-MM-dd'T'hh-mm-ss");
		datetimeInput.setOffset("P1D");

		CombinedInput combinedInput = new CombinedInput();
		combinedInput.setId("Input_combined");
		combinedInput.setFormat("(1)-(2): (3)");
		combinedInput.setInputs(new ArrayList<>());
		combinedInput.getInputs().add(randomNumberInput);
		combinedInput.getInputs().add(listInput);
		combinedInput.getInputs().add(randomStringInput);

		annotation.addInput(listInput);
		annotation.addInput(jsonInput);
		annotation.addInput(randomNumberInput);
		annotation.addInput(randomStringInput);
		annotation.addInput(datetimeInput);
		annotation.addInput(combinedInput);
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
