package org.continuity.idpa.serialization.json;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.serialization.JsonItemSaniDeserializer;
import org.continuity.idpa.serialization.yaml.IdpaDeserializer;
import org.continuity.idpa.serialization.yaml.IdpaElementMixin;
import org.continuity.idpa.serialization.yaml.IdpaSerializer;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.continuity.idpa.serialization.yaml.PropertyOverrideDeserializer;
import org.continuity.idpa.serialization.yaml.PropertyOverrideSerializer;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 * Utils for (de)serialization of IDPA elements. Provides default {@link ObjectMapper}s, which
 * should be used if possible for performance reasons.
 *
 * @author Henning Schulz
 *
 */
public class IdpaSerializationUtils {

	private static ObjectMapper jsonMapper;

	private static ObjectMapper yamlMapper;

	private IdpaSerializationUtils() {
	}

	/**
	 * Gets a default {@link ObjectMapper}, which is able to (de)serialize IDPA elements in JSON.
	 *
	 * @return
	 */
	public static ObjectMapper getDefaultJsonObjectMapper() {
		if (jsonMapper == null) {
			synchronized (IdpaSerializationUtils.class) {
				if (jsonMapper == null) {
					jsonMapper = new ObjectMapper();
					configureObjectMapper(jsonMapper);
				}
			}
		}

		return jsonMapper;
	}

	/**
	 * Gets a default {@link ObjectMapper}, which is able to (de)serialize IDPA elements in YAML.
	 * This mapper is also used by the {@link IdpaYamlSerializer}.
	 *
	 * @return
	 */
	public static ObjectMapper getDefaultYamlObjectMapper() {
		if (yamlMapper == null) {
			synchronized (IdpaSerializationUtils.class) {
				if (yamlMapper == null) {
					yamlMapper = new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).enable(Feature.USE_NATIVE_OBJECT_ID));

					configureObjectMapper(yamlMapper);

					yamlMapper.addMixIn(AbstractIdpaElement.class, IdpaElementMixin.class);
					yamlMapper.registerModule(new SimpleModule().setSerializerModifier(new ContinuitySerializerModifier()));
					yamlMapper.registerModule(new SimpleModule().addSerializer(getPropertyOverrideClass(), new PropertyOverrideSerializer()));

					yamlMapper.registerModule(new SimpleModule().setDeserializerModifier(new ContinuityDeserializerModifier()));
					yamlMapper.registerModule(new SimpleModule().addDeserializer(PropertyOverride.class, new PropertyOverrideDeserializer()));
				}
			}
		}

		return yamlMapper;
	}

	/**
	 * Configures an existing {@link ObjectMapper} for proper (de)serialization of IDPA elements in
	 * JSON.
	 *
	 * @param mapper
	 */
	public static void configureObjectMapper(ObjectMapper mapper) {
		mapper.registerModule(new SimpleModule().setDeserializerModifier(new JsonItemSaniDeserializerModifier()));
	}

	@SuppressWarnings("unchecked")
	private static Class<PropertyOverride<?>> getPropertyOverrideClass() {
		Class<?> clazz = PropertyOverride.class;
		return (Class<PropertyOverride<?>>) clazz;
	}

	private static class ContinuitySerializerModifier extends BeanSerializerModifier {

		@SuppressWarnings("unchecked")
		@Override
		public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
			if (IdpaElement.class.isAssignableFrom(beanDesc.getBeanClass())) {
				return new IdpaSerializer((JsonSerializer<Object>) serializer);
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
			if (IdpaElement.class.isAssignableFrom(beanDesc.getBeanClass())) {
				return new IdpaDeserializer((JsonDeserializer<Object>) deserializer);
			}

			return deserializer;
		}

	}

	private static class JsonItemSaniDeserializerModifier extends BeanDeserializerModifier {

		@SuppressWarnings("unchecked")
		@Override
		public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
			if (ApplicationAnnotation.class.equals(beanDesc.getBeanClass())) {
				return new JsonItemSaniDeserializer((JsonDeserializer<ApplicationAnnotation>) deserializer);
			}

			return deserializer;
		}

	}

}
