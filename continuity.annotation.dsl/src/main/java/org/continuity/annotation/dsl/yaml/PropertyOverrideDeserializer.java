package org.continuity.annotation.dsl.yaml;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.PropertyOverride;
import org.continuity.annotation.dsl.ann.PropertyOverrideKey;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author Henning Schulz
 *
 */
public class PropertyOverrideDeserializer extends JsonDeserializer<PropertyOverride<?>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PropertyOverride<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		p.getText(); // Start token
		p.nextToken();
		String key = p.getText();
		p.nextToken();
		String value = p.getText();
		p.nextToken();
		p.getText(); // End token

		return createOverride(key, value);
	}

	@SuppressWarnings("unchecked")
	private <T extends PropertyOverrideKey.Any> PropertyOverride<T> createOverride(String key, String value) {
		PropertyOverride<T> override = new PropertyOverride<>();
		override.setKey((T) PropertyOverrideKey.fromPrintableString(key));
		override.setValue(value);
		return override;
	}

}
