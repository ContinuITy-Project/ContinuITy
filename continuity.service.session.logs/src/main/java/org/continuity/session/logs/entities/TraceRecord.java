package org.continuity.session.logs.entities;

import java.io.IOException;

import org.continuity.idpa.VersionOrTimestamp;
import org.spec.research.open.xtrace.api.core.Trace;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import open.xtrace.OPENxtraceUtils;

public class TraceRecord {

	private VersionOrTimestamp version;

	@JsonSerialize(using = TraceSerializer.class)
	@JsonDeserialize(using = TraceDeserializer.class)
	private Trace trace;

	public TraceRecord() {
	}

	public TraceRecord(VersionOrTimestamp version, Trace trace) {
		this.version = version;
		this.trace = trace;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public Trace getTrace() {
		return trace;
	}

	public void setTrace(Trace trace) {
		this.trace = trace;
	}

	public static class TraceSerializer extends StdSerializer<Trace> {

		private static final long serialVersionUID = -5597787849884279751L;

		public TraceSerializer() {
			super(Trace.class);
		}

		@Override
		public void serialize(Trace trace, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeRawValue(OPENxtraceUtils.serializeTraceToJsonString(trace));
		}

	}

	public static class TraceDeserializer extends StdDeserializer<Trace> {

		private static final long serialVersionUID = 4960837171316048586L;

		protected TraceDeserializer() {
			super(Trace.class);
		}

		@Override
		public Trace deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			return OPENxtraceUtils.deserializeToTrace(p.readValueAsTree().toString());
		}

	}

}
