package org.continuity.idpa.application.entities;

import java.io.IOException;

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
 * @author Henning Schulz
 *
 */
@JsonSerialize(using = ApplicationChangeType.JsonSerializer.class)
@JsonDeserialize(using = ApplicationChangeType.JsonDeserializer.class)
public enum ApplicationChangeType {

	ENDPOINT_CHANGED("The endpoint has changed."), ENDPOINT_REMOVED("The endpoint has been removed."), ENDPOINT_ADDED("A new endpoint has been added."), PARAMETER_CHANGED(
			"The parameter has changed."), PARAMETER_REMOVED(
			"The parameter has been removed."), PARAMETER_ADDED("A new parameter has been added.");

	private final String prettyName;

	private final String description;

	private ApplicationChangeType(String description) {
		String tmp = name().replace("_", " ").toLowerCase();
		this.prettyName = tmp.substring(0, 1).toUpperCase() + tmp.substring(1);
		this.description = description;
	}

	/**
	 * Gets {@link #prettyName}.
	 *
	 * @return {@link #prettyName}
	 */
	public String prettyName() {
		return this.prettyName;
	}

	/**
	 * Gets {@link #description}.
	 *
	 * @return {@link #description}
	 */
	public String description() {
		return this.description;
	}

	public String getMessage() {
		return prettyName() + ": " + description();
	}

	protected static class JsonSerializer extends StdSerializer<ApplicationChangeType> {

		private static final long serialVersionUID = -3584135264314110782L;

		public JsonSerializer() {
			this(null);
		}

		public JsonSerializer(Class<ApplicationChangeType> t) {
			super(t);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(ApplicationChangeType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(value.getMessage());
		}

	}

	protected static class JsonDeserializer extends StdDeserializer<ApplicationChangeType> {

		private static final long serialVersionUID = 163275591308563017L;

		public JsonDeserializer() {
			this(null);
		}

		/**
		 * @param vc
		 */
		protected JsonDeserializer(Class<?> vc) {
			super(vc);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ApplicationChangeType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			String prettyName = p.getValueAsString().split("\\:")[0];
			String name = prettyName.toUpperCase().replace(" ", "_");
			return ApplicationChangeType.valueOf(name);
		}

	}

}
