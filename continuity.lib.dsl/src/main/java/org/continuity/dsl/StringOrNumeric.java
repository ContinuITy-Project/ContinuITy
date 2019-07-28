package org.continuity.dsl;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Automatically determines whether the deserialized field is a string or a number and serializes it
 * accordingly.
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(using = StringOrNumeric.Serializer.class)
@JsonDeserialize(using = StringOrNumeric.Deserializer.class)
public class StringOrNumeric {

	private Optional<String> stringValue;

	private Optional<Double> doubleValue;

	/**
	 * Creates a numerical instance.
	 * 
	 * @param value
	 */
	public StringOrNumeric(double value) {
		this(null, value);
	}

	/**
	 * Creates a string instance.
	 * 
	 * @param value
	 */
	public StringOrNumeric(String value) {
		this(value, null);
	}

	/**
	 * Creates a {@code null} instance.
	 */
	public StringOrNumeric() {
		this(null, null);
	}

	private StringOrNumeric(String stringValue, Double doubleValue) {
		this.stringValue = Optional.ofNullable(stringValue);
		this.doubleValue = Optional.ofNullable(doubleValue);
	}

	public double getAsNumber() {
		if (!isNumeric()) {
			throw new IllegalStateException("Cannot get string field as number!");
		}

		return doubleValue.get();
	}

	public String getAsString() {
		if (!isString()) {
			throw new IllegalStateException("Cannot get numeric field as string!");
		}

		return stringValue.get();
	}

	public boolean isNumeric() {
		return doubleValue.isPresent();
	}

	public boolean isString() {
		return stringValue.isPresent();
	}

	public boolean isNull() {
		return !isNumeric() && !isString();
	}

	public static class Serializer extends StdSerializer<StringOrNumeric> {

		private static final long serialVersionUID = 1644078031650138946L;

		protected Serializer() {
			super(StringOrNumeric.class);
		}

		@Override
		public void serialize(StringOrNumeric value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			if (value.isNumeric()) {
				gen.writeNumber(value.getAsNumber());
			} else if (value.isString()) {
				gen.writeString(value.getAsString());
			} else {
				gen.writeNull();
			}
		}

	}

	public static class Deserializer extends StdDeserializer<StringOrNumeric> {

		private static final long serialVersionUID = -2929243696707998274L;

		protected Deserializer() {
			super(StringOrNumeric.class);
		}

		@Override
		public StringOrNumeric deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = p.getCodec().readTree(p);

			if (node.isNumber()) {
				return new StringOrNumeric(null, node.asDouble());
			} else if (node.isTextual()) {
				return new StringOrNumeric(node.asText(), null);
			} else {
				return new StringOrNumeric(null, null);
			}
		}

	}

}
