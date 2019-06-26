package org.continuity.idpa.serialization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvColumnInput;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.extracted.EndpointOrInput;
import org.continuity.idpa.serialization.yaml.CsvColumnInputSerializer;
import org.continuity.idpa.serialization.yaml.EndpointOrInputSerializer;
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
					yamlMapper.registerModule(new SimpleModule().addSerializer(CsvColumnInput.class, new CsvColumnInputSerializer()));
					yamlMapper.registerModule(new SimpleModule().addSerializer(EndpointOrInput.class, new EndpointOrInputSerializer()));

					yamlMapper.registerModule(new SimpleModule().setDeserializerModifier(new ContinuityDeserializerModifier()));
					yamlMapper.registerModule(new SimpleModule().setDeserializerModifier(new ExtractedInputIdSaniDeserializerModifier()));
					yamlMapper.registerModule(new SimpleModule().addDeserializer(PropertyOverride.class, new PropertyOverrideDeserializer()));
				}
			}
		}

		return yamlMapper;
	}

	/**
	 * Gets a list of {@link PreDeserializationSanitizer}, which are to be called before putting a
	 * yaml String into the deserializer.
	 *
	 * @return The list of {@link PreDeserializationSanitizer}.
	 */
	public static List<PreDeserializationSanitizer> getPreDeserializationSanitizers() {
		List<PreDeserializationSanitizer> sanitizers = new ArrayList<>();

		sanitizers.add(IdpaSerializationUtils::sanitizeForCsvColumnInput);
		sanitizers.add(IdpaSerializationUtils::sanitizeForValueExtraction);

		return sanitizers;
	}

	/**
	 * Applies all {@link PreDeserializationSanitizer} returned by
	 * {@link #getPreDeserializationSanitizers()} to the input string.
	 *
	 * @param yaml
	 *            The input string to be sanitized.
	 * @return The sanitized string.
	 */
	public static String sanitizeBeforeDeserializing(String yaml) {
		for (PreDeserializationSanitizer sanitizer : getPreDeserializationSanitizers()) {
			yaml = sanitizer.sanitize(yaml);
		}

		return yaml;
	}

	private static String sanitizeForCsvColumnInput(String yaml) {
		Matcher matcher = Pattern.compile("( +columns:)( *\\n +- &[a-zA-Z0-9_]+)+").matcher(yaml);

		StringBuilder sanitizedYaml = new StringBuilder();

		int lastEnd = 0;

		while (matcher.find()) {
			String columnsYaml = matcher.group();
			int start = matcher.start();
			int end = matcher.end();

			sanitizedYaml.append(yaml.substring(lastEnd, start));
			sanitizedYaml.append(matcher.group(1));

			Matcher subMatcher = Pattern.compile("\\n +- &[a-zA-Z0-9_]+ *").matcher(columnsYaml);

			while (subMatcher.find()) {
				sanitizedYaml.append(subMatcher.group());
				sanitizedYaml.append(" content: ");
				sanitizedYaml.append(CsvColumnInput.DUMMY_CONTENT);
			}

			lastEnd = end;
		}

		sanitizedYaml.append(yaml.substring(lastEnd));

		return sanitizedYaml.toString();
	}

	private static String sanitizeForValueExtraction(String yaml) {
		Matcher matcher = Pattern.compile("\\n( +)(- |  )(from:) *(\\*?[a-zA-Z0-9_\\.]+)").matcher(yaml);

		StringBuilder sanitizedYaml = new StringBuilder();

		int lastEnd = 0;

		while (matcher.find()) {
			int start = matcher.start();
			String indent = matcher.group(1);
			String endpointOrId = matcher.group(4);

			sanitizedYaml.append(yaml.substring(lastEnd, start)).append("\n");
			sanitizedYaml.append(indent).append(matcher.group(2)).append(matcher.group(3));
			indent += "  ";

			sanitizedYaml.append("\n").append(indent);

			if (endpointOrId.startsWith("*")) {
				sanitizedYaml.append("  rawInputId: ").append(endpointOrId.substring(1));
			} else {
				String[] endpointAndKey = endpointOrId.split("\\.");
				sanitizedYaml.append("  endpoint: ").append(endpointAndKey[0]);

				if (endpointAndKey.length > 1) {
					sanitizedYaml.append("\n").append(indent).append("  response-key: ").append(endpointAndKey[1]);
				}
			}

			lastEnd = matcher.end();
		}

		sanitizedYaml.append(yaml.substring(lastEnd));

		return sanitizedYaml.toString();
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

	private static class ExtractedInputIdSaniDeserializerModifier extends BeanDeserializerModifier {

		@SuppressWarnings("unchecked")
		@Override
		public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
			if (ApplicationAnnotation.class.equals(beanDesc.getBeanClass())) {
				return new ExtractedInputIdSaniDeserializer((JsonDeserializer<ApplicationAnnotation>) deserializer);
			}

			return deserializer;
		}

	}

}
