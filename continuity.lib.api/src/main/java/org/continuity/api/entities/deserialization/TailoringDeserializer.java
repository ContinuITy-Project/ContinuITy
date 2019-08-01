package org.continuity.api.entities.deserialization;

import java.io.IOException;
import java.util.List;

import org.continuity.api.entities.artifact.session.Session;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 *
 * @author Henning Schulz
 *
 */
public class TailoringDeserializer extends StdDeserializer<List<String>> {

	private static final long serialVersionUID = -3619765155269279344L;

	protected TailoringDeserializer() {
		super(List.class);
	}

	@Override
	public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return Session.convertStringToTailoring(p.getValueAsString());
	}

}
