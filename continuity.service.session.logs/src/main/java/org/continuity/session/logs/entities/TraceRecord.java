package org.continuity.session.logs.entities;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.HttpEndpoint;
import org.spec.research.open.xtrace.api.core.Trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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

@JsonPropertyOrder({ "endpoint", "version", "unique-session-ids", "cluster-id", "trace" })
public class TraceRecord {

	@JsonSerialize(using = VersionOrTimestamp.NormalizedSerializer.class)
	private VersionOrTimestamp version;

	private String endpoint;

	@JsonIgnore
	private HttpEndpoint rawEndpoint;

	@JsonProperty("unique-session-ids")
	private Set<String> uniqueSessionIds;

	@JsonProperty("cluster-id")
	@JsonInclude(Include.NON_ABSENT)
	private Optional<Long> clusterId;

	@JsonSerialize(using = TraceSerializer.class)
	@JsonDeserialize(using = TraceDeserializer.class)
	private Trace trace;

	public TraceRecord() {
	}

	public TraceRecord(VersionOrTimestamp version, Trace trace) {
		this.version = version;
		this.trace = trace;
		this.uniqueSessionIds = new HashSet<>();
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public HttpEndpoint getRawEndpoint() {
		return rawEndpoint;
	}

	public void setRawEndpoint(HttpEndpoint rawEndpoint) {
		this.rawEndpoint = rawEndpoint;
		this.endpoint = rawEndpoint.getId();
	}

	public Set<String> getUniqueSessionIds() {
		return uniqueSessionIds;
	}

	public void setUniqueSessionIds(Set<String> uniqueSessionIds) {
		this.uniqueSessionIds = uniqueSessionIds;
	}

	public void addUniqueSessionIds(Set<String> uniqueSessionIds) {
		if (uniqueSessionIds == null) {
			uniqueSessionIds = new HashSet<>();
		}

		this.uniqueSessionIds.addAll(uniqueSessionIds);
	}

	public Optional<Long> getClusterId() {
		return clusterId;
	}

	public void setClusterId(Optional<Long> clusterId) {
		this.clusterId = clusterId;
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
