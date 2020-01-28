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
 * Automatically determines whether the deserialized field is a string, boolean, or a number and
 * serializes it accordingly.
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(using = ContextValue.Serializer.class)
@JsonDeserialize(using = ContextValue.Deserializer.class)
public class ContextValue {

	private Optional<String> stringValue;

	private Optional<Double> doubleValue;

	private Optional<Boolean> booleanValue;

	/**
	 * Creates a numerical instance.
	 *
	 * @param value
	 */
	public ContextValue(double value) {
		this(null, value, null);
	}

	/**
	 * Creates a string instance.
	 *
	 * @param value
	 */
	public ContextValue(String value) {
		this(value, null, null);
	}

	/**
	 * Creates a boolean value.
	 *
	 * @param value
	 */
	public ContextValue(boolean value) {
		this(null, null, value);
	}

	/**
	 * Creates a {@code null} instance.
	 */
	public ContextValue() {
		this(null, null, null);
	}

	private ContextValue(String stringValue, Double doubleValue, Boolean booleanValue) {
		this.stringValue = Optional.ofNullable(stringValue);
		this.doubleValue = Optional.ofNullable(doubleValue);
		this.booleanValue = Optional.ofNullable(booleanValue);
	}

	public double getAsNumber() {
		if (!isNumeric()) {
			throw new IllegalStateException("This is not a number!");
		}

		return doubleValue.get();
	}

	public String getAsString() {
		if (!isString()) {
			throw new IllegalStateException("This is not a string!");
		}

		return stringValue.get();
	}

	public boolean getAsBoolean() {
		if (!isBoolean()) {
			throw new IllegalStateException("This is not a boolean!");
		}

		return booleanValue.get();
	}

	public boolean isNumeric() {
		return doubleValue.isPresent();
	}

	public boolean isString() {
		return stringValue.isPresent();
	}

	public boolean isBoolean() {
		return booleanValue.isPresent();
	}

	public boolean isNull() {
		return !isNumeric() && !isString() && !isBoolean();
	}

	public static class Serializer extends StdSerializer<ContextValue> {

		private static final long serialVersionUID = 1644078031650138946L;

		protected Serializer() {
			super(ContextValue.class);
		}

		@Override
		public void serialize(ContextValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			if (value.isNumeric()) {
				gen.writeNumber(value.getAsNumber());
			} else if (value.isString()) {
				gen.writeString(value.getAsString());
			} else if (value.isBoolean()) {
				gen.writeBoolean(value.getAsBoolean());
			} else {
				gen.writeNull();
			}
		}

	}

	public static class Deserializer extends StdDeserializer<ContextValue> {

		private static final long serialVersionUID = -2929243696707998274L;

		protected Deserializer() {
			super(ContextValue.class);
		}

		@Override
		public ContextValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = p.getCodec().readTree(p);

			if (node.isNumber()) {
				return new ContextValue(node.asDouble());
			} else if (node.isTextual()) {
				return new ContextValue(node.asText());
			} else if (node.isBoolean()) {
				return new ContextValue(node.asBoolean());
			} else {
				return new ContextValue();
			}
		}

	}

}
