package org.continuity.api.entities.deserialization;

import java.io.IOException;
import java.util.List;

import org.continuity.api.entities.artifact.session.Session;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author Henning Schulz
 *
 */
public class TailoringSerializer extends StdSerializer<List<String>> {

	private static final long serialVersionUID = -4973460573775064579L;

	@SuppressWarnings("unchecked")
	public TailoringSerializer() {
		super((Class<List<String>>) (Class<?>) List.class);
	}

	@Override
	public void serialize(List<String> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(Session.convertTailoringToString(value));
	}

}
