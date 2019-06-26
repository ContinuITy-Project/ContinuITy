package org.continuity.idpa.serialization.yaml;

import java.io.IOException;

import org.continuity.idpa.annotation.PropertyOverride;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author Henning Schulz
 *
 */
public class PropertyOverrideSerializer extends JsonSerializer<PropertyOverride<?>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(PropertyOverride<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeStartObject();
		gen.writeFieldName(value.getKey().toString());
		gen.writeString(value.getValue());
		gen.writeEndObject();
	}

}
