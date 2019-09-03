package org.continuity.dsl.elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Specifies how to aggregate the intensity time series.
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(using = TypedProperties.Serializer.class)
@JsonDeserialize(using = TypedProperties.Deserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TypedProperties {

	private String type;

	private Map<String, Object> properties;

	public String getType() {
		return type;
	}

	public TypedProperties setType(String type) {
		this.type = type;
		return this;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public TypedProperties setProperties(Map<String, Object> properties) {
		this.properties = properties;
		return this;
	}

	public static class Serializer extends StdSerializer<TypedProperties> {

		private static final long serialVersionUID = -5314514273591648456L;

		protected Serializer() {
			super(TypedProperties.class);
		}

		@Override
		public void serialize(TypedProperties value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			if (gen.canWriteTypeId()) {
				gen.writeTypeId(value.getType());
			}

			gen.writeStartObject();

			if (!gen.canWriteTypeId()) {
				gen.writeFieldName("@type");
				gen.writeString(value.getType());
			}

			if (value.getProperties() != null) {
				for (Entry<String, Object> prop : value.getProperties().entrySet()) {
					gen.writeFieldName(prop.getKey());

					if (prop.getValue() == null) {
						gen.writeNull();
					} else if (prop.getValue() instanceof Number) {
						if ((prop.getValue() instanceof Double) || (prop.getValue() instanceof Float)) {
							gen.writeNumber(((Number) prop.getValue()).doubleValue());
						} else {
							gen.writeNumber(((Number) prop.getValue()).longValue());
						}
					} else if (prop.getValue() instanceof Boolean) {
						gen.writeBoolean((Boolean) prop.getValue());
					} else {
						gen.writeString(Objects.toString(prop.getValue()));
					}
				}
			}

			gen.writeEndObject();
		}

	}

	public static class Deserializer extends StdDeserializer<TypedProperties> {

		private static final long serialVersionUID = -895105823269513034L;

		protected Deserializer() {
			super(TypedProperties.class);
		}

		@Override
		public TypedProperties deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			Object type = null;

			if (p.canReadTypeId()) {
				type = p.getTypeId();
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> properties = p.readValueAs(HashMap.class);

			if (!p.canReadTypeId()) {
				type = properties.remove("@type");
			}

			return new TypedProperties().setType(type.toString()).setProperties(properties);
		}

	}

}
