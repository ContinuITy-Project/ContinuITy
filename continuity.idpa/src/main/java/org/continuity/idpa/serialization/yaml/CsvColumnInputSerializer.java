package org.continuity.idpa.serialization.yaml;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.continuity.idpa.annotation.CsvColumnInput;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author Henning Schulz
 *
 */
public class CsvColumnInputSerializer extends StdSerializer<CsvColumnInput> {

	private static final long serialVersionUID = 8451297070474483602L;

	private final Set<CsvColumnInput> serialized = new HashSet<>();

	public CsvColumnInputSerializer(Class<CsvColumnInput> t) {
		super(t);
	}

	public CsvColumnInputSerializer() {
		super(CsvColumnInput.class);
	}

	@Override
	public void serialize(CsvColumnInput value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (serialized.contains(value)) {
			gen.writeObjectRef(value.getId());
		} else {
			if (gen.canWriteObjectId()) {
				gen.writeObjectId(value.getId());
				gen.writeString("");
				serialized.add(value);
			} else {
				gen.writeStartObject();
				gen.writeStringField("id", value.getId());
				gen.writeEndObject();
			}
		}
	}

	@Override
	public void serializeWithType(CsvColumnInput value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		serialize(value, gen, serializers);
	}

}
