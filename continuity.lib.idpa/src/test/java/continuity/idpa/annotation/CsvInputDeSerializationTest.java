package continuity.idpa.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvColumnInput;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.serialization.IdpaSerializationUtils;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CsvInputDeSerializationTest {

	private ApplicationAnnotation annotation;

	private IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

	private ObjectMapper jsonMapper = IdpaSerializationUtils.getDefaultJsonObjectMapper();

	@Before
	public void setup() {
		annotation = new ApplicationAnnotation();

		CsvInput input = new CsvInput();
		input.setFilename("data.csv");
		input.setColumns(new ArrayList<>());
		input.setHeader(true);
		CsvColumnInput col1 = new CsvColumnInput();
		CsvColumnInput col2 = new CsvColumnInput();
		col1.setId("Input_username");
		col2.setId("Input_password");
		input.getColumns().add(col1);
		input.getColumns().add(col2);
		annotation.getInputs().add(input);

		input = new CsvInput();
		input.setFilename("data.csv");
		input.setColumns(new ArrayList<>());
		col1 = new CsvColumnInput();
		col1.setId("Input_foo");
		input.getColumns().add(col1);
		annotation.getInputs().add(input);

		input = new CsvInput();
		input.setId("Input_oldCsv");
		input.setFilename("data.csv");
		input.setColumn(1);
		annotation.getInputs().add(input);

		EndpointAnnotation endpAnn = new EndpointAnnotation();
		endpAnn.setAnnotatedEndpoint(WeakReference.create(Endpoint.GENERIC_TYPE, "login"));
		ParameterAnnotation paramAnn = new ParameterAnnotation();
		paramAnn.setAnnotatedParameter(WeakReference.create(Parameter.class, "username"));
		paramAnn.setInput(col1);
		endpAnn.addParameterAnnotation(paramAnn);

		annotation.getEndpointAnnotations().add(endpAnn);
	}

	@Test
	public void testWriteReadYaml() throws IOException {
		String yaml = serializer.writeToYamlString(annotation);
		ApplicationAnnotation parsed = serializer.readFromYamlString(yaml);
		String newYaml = serializer.writeToYamlString(parsed);

		assertThat(newYaml).isEqualTo(yaml);
	}

	@Test
	public void testReadWriteYaml() throws JsonParseException, JsonMappingException, IOException {
		// DON'T TOUCH THE FILE!!!
		String yaml = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/annotation-csv.yml"))).lines().collect(Collectors.joining("\n"));
		ApplicationAnnotation ann = serializer.readFromYamlInputStream(getClass().getResourceAsStream("/annotation-csv.yml"));

		String newYaml = serializer.writeToYamlString(ann);

		assertThat(newYaml).isEqualTo(yaml);
	}

	@Test
	public void testWriteReadJson() throws IOException {
		String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(annotation);
		ApplicationAnnotation parsed = jsonMapper.readValue(json, ApplicationAnnotation.class);
		String newJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);

		assertThat(newJson).isEqualTo(json);
	}

}
