package org.continuity.api.entities.report;

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
@JsonSerialize(using = AnnotationViolationType.JsonSerializer.class)
@JsonDeserialize(using = AnnotationViolationType.JsonDeserializer.class)
public enum AnnotationViolationType {

	ILLEGAL_ENDPOINT_REFERENCE("The reference to the endpoint is not valid.", true), ILLEGAL_PARAMETER_REFERENCE("The reference to the parameter is not valid.", true),

	ILLEGAL_INTERNAL_REFERENCE("The internal reference is not valid.", true);

	private final String prettyName;

	private final String description;

	private final boolean breaking;

	private AnnotationViolationType(String description, boolean breaking) {
		String tmp = name().replace("_", " ").toLowerCase();
		this.prettyName = tmp.substring(0, 1).toUpperCase() + tmp.substring(1);
		this.description = description;
		this.breaking = breaking;
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

	public boolean isBreaking() {
		return breaking;
	}

	protected static class JsonSerializer extends StdSerializer<AnnotationViolationType> {

		private static final long serialVersionUID = -3584135264314110782L;

		public JsonSerializer() {
			this(null);
		}

		public JsonSerializer(Class<AnnotationViolationType> t) {
			super(t);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(AnnotationViolationType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(value.getMessage());
		}

	}

	protected static class JsonDeserializer extends StdDeserializer<AnnotationViolationType> {

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
		public AnnotationViolationType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			String prettyName = p.getValueAsString().split("\\:")[0];
			String name = prettyName.toUpperCase().replace(" ", "_");
			return AnnotationViolationType.valueOf(name);
		}

	}

}
