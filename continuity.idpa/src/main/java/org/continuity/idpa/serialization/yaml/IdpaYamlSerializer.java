package org.continuity.idpa.serialization.yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.serialization.json.IdpaSerializationUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * @author Henning Schulz
 *
 */
public class IdpaYamlSerializer<T extends IdpaElement> {

	private final Class<T> type;

	private final ObjectMapper mapper;

	private final ObjectWriter writer;

	/**
	 *
	 */
	public IdpaYamlSerializer(Class<T> type) {
		this.type = type;

		mapper = IdpaSerializationUtils.getDefaultYamlObjectMapper();

		writer = mapper.writer(new SimpleFilterProvider().addFilter("idFilter", new IdFilter()));
	}

	public T readFromYaml(File yamlSource) throws JsonParseException, JsonMappingException, IOException {
		T read = mapper.readValue(yamlSource, type);
		return read;
	}

	public T readFromYaml(String yamlSource) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(new File(yamlSource));
	}

	public T readFromYaml(URL yamlSource) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(yamlSource.getPath());
	}

	public T readFromYaml(Path yamlPath) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(yamlPath.toString());
	}

	public T readFromYamlInputStream(InputStream inputStream) throws JsonParseException, JsonMappingException, IOException {
		T read = mapper.readValue(inputStream, type);
		return read;
	}

	public T readFromYamlString(String yamlString) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(yamlString, type);
	}

	public T readFromJsonNode(JsonNode root) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readerFor(type).readValue(root);
	}

	public void writeToYaml(T model, File yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		writer.writeValue(yamlFile, model);
	}

	public void writeToYaml(T model, String yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, new File(yamlFile));
	}

	public void writeToYaml(T model, URL yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, yamlFile.getPath());
	}

	public void writeToYaml(T model, Path yamlPath) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, yamlPath.toString());
	}

	public String writeToYamlString(T model) throws JsonProcessingException {
		return writer.writeValueAsString(model);
	}

	private static class IdFilter extends SimpleBeanPropertyFilter {

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
			if (include(writer)) {
				if (!writer.getName().equals("id")) {
					writer.serializeAsField(pojo, jgen, provider);
					return;
				}
			} else if (!jgen.canOmitFields()) { // since 2.3
				writer.serializeAsOmittedField(pojo, jgen, provider);
			}
		}

		@Override
		protected boolean include(BeanPropertyWriter writer) {
			return true;
		}

		@Override
		protected boolean include(PropertyWriter writer) {
			return true;
		}
	}

}
