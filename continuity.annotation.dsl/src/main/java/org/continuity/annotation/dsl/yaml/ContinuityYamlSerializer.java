package org.continuity.annotation.dsl.yaml;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;
import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.PropertyOverride;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
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
public class ContinuityYamlSerializer<T extends ContinuityModelElement> {

	private final Class<T> type;

	/**
	 *
	 */
	public ContinuityYamlSerializer(Class<T> type) {
		this.type = type;
	}

	public T readFromYaml(String yamlSource) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
		mapper.registerModule(new SimpleModule().setDeserializerModifier(new ContinuityDeserializerModifier()));

		mapper.registerModule(new SimpleModule().addDeserializer(WeakReference.class, new WeakReferenceDeserializer()));
		mapper.registerModule(new SimpleModule().addDeserializer(PropertyOverride.class, new PropertyOverrideDeserializer()));
		T read = mapper.readValue(new File(yamlSource), type);
		ModelValidator.fixAll(read);
		return read;
	}

	public T readFromYaml(URL yamlSource) throws JsonParseException, JsonMappingException, IOException {
		return readFromYaml(yamlSource.getPath());
	}

	public void writeToYaml(T model, String yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));
		mapper.addMixIn(AbstractContinuityModelElement.class, ContinuityModelElementMixin.class);
		mapper.registerModule(new SimpleModule().setSerializerModifier(new ContinuitySerializerModifier()));
		mapper.registerModule(new SimpleModule().addSerializer(getPropertyOverrideClass(), new PropertyOverrideSerializer()));
		mapper.writer(new SimpleFilterProvider().addFilter("idFilter", new IdFilter())).writeValue(new File(yamlFile), model);
	}

	public void writeToYaml(T model, URL yamlFile) throws JsonGenerationException, JsonMappingException, IOException {
		writeToYaml(model, yamlFile.getPath());
	}

	@SuppressWarnings("unchecked")
	private Class<PropertyOverride<?>> getPropertyOverrideClass() {
		Class<?> clazz = PropertyOverride.class;
		return (Class<PropertyOverride<?>>) clazz;
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

	private static class ContinuityDeserializerModifier extends BeanDeserializerModifier {

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
			if (ContinuityModelElement.class.isAssignableFrom(beanDesc.getBeanClass())) {
				return new ContinuityModelDeserializer((JsonDeserializer<Object>) deserializer);
			}

			return deserializer;
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
