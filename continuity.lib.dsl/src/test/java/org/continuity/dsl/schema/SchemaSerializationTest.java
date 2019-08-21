package org.continuity.dsl.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class SchemaSerializationTest {

	private ContextSchema schema;

	private ObjectMapper mapper;

	@Before
	public void setup() {
		schema = new ContextSchema();
		schema.setAutoDetect(true);

		Map<String, VariableSchema> variables = new HashMap<>();
		variables.put("string_var", new VariableSchema(VariableType.STRING, false));
		variables.put("num_var", new VariableSchema(VariableType.NUMERIC, true));
		variables.put("bool_var", new VariableSchema(VariableType.BOOLEAN));

		schema.setVariables(variables);

		mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID)).registerModule(new Jdk8Module());
	}

	@Test
	public void testWriteRead() throws IOException {
		String yaml = mapper.writeValueAsString(schema);

		System.out.println(yaml);

		ContextSchema parsed = mapper.readValue(yaml, ContextSchema.class);

		assertThat(parsed.getAutoDetect()).isTrue();
		assertThat(parsed.getIgnoreByDefault()).isEmpty();
		assertThat(parsed.getVariables().keySet()).containsExactlyInAnyOrder("string_var", "num_var", "bool_var");

		assertThat(parsed.getVariables().get("string_var").getType()).isEqualTo(VariableType.STRING);
		assertThat(parsed.getVariables().get("string_var").getIgnoreByDefault()).contains(false);

		assertThat(parsed.getVariables().get("num_var").getType()).isEqualTo(VariableType.NUMERIC);
		assertThat(parsed.getVariables().get("num_var").getIgnoreByDefault()).contains(true);

		assertThat(parsed.getVariables().get("bool_var").getType()).isEqualTo(VariableType.BOOLEAN);
		assertThat(parsed.getVariables().get("bool_var").getIgnoreByDefault()).isEmpty();
	}

}
