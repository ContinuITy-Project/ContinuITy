package org.continuity.idpa.serialization.yaml;

import java.io.IOException;

import org.continuity.idpa.annotation.extracted.EndpointOrInput;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author Henning Schulz
 *
 */
public class EndpointOrInputSerializer extends StdSerializer<EndpointOrInput> {

	private static final long serialVersionUID = 7635255747765341605L;

	public EndpointOrInputSerializer(Class<EndpointOrInput> t) {
		super(t);
	}

	public EndpointOrInputSerializer() {
		super(EndpointOrInput.class);
	}

	@Override
	public void serialize(EndpointOrInput value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value.isEndpoint()) {
			String epString = value.getEndpoint().getId();

			if (value.getResponseKey() != null) {
				epString += "." + value.getResponseKey();
			}

			gen.writeString(epString);
		} else if (value.isInput()) {
			gen.writeObjectRef(value.getInput().getId());
		} else {
			gen.writeNull();
		}
	}

}
