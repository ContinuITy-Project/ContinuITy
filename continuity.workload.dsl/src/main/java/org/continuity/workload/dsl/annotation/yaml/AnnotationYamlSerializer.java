package org.continuity.workload.dsl.annotation.yaml;

import java.io.File;
import java.io.IOException;

import org.continuity.workload.dsl.ContinuityModelElement;
import org.continuity.workload.dsl.annotation.SystemAnnotation;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationYamlSerializer {

	public SystemAnnotation readFromYaml(String yamlSource) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
		return mapper.readValue(yamlSource, SystemAnnotation.class);
	}

	public void writeToYaml(SystemAnnotation annotation, String yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
		mapper.registerModule(new SimpleModule().setSerializerModifier(new ContinuitySerializerModifier()));
		mapper.writer(new SimpleFilterProvider().addFilter("idFilter", new IdFilter())).writeValue(new File(yamlFile), annotation);
	}

	private static class ContinuitySerializerModifier extends BeanSerializerModifier {

		@SuppressWarnings("unchecked")
		@Override
		public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
			if (ContinuityModelElement.class.isAssignableFrom(beanDesc.getBeanClass())) {
				return new ContinuityModelSerializer((JsonSerializer<Object>) serializer);
			}

			return serializer;
		}

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
