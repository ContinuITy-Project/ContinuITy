package org.continuity.api.entities.artifact.session;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
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
 * Represents a session.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "unique-id", "session-id", "version", "start-micros", "end-micros", "finished", "tailoring", "requests" })
@JsonView(SessionView.Simple.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

	private static final String DELIM = ";";

	@JsonProperty("session-id")
	private String sessionId;

	private VersionOrTimestamp version;

	@JsonProperty("start-micros")
	private long startMicros = Long.MAX_VALUE;

	@JsonProperty("end-micros")
	private long endMicros;

	private boolean finished;

	@JsonIgnore
	private boolean fresh;

	@JsonSerialize(using = TailoringSerializer.class)
	@JsonDeserialize(using = TailoringDeserializer.class)
	@JsonView(SessionView.Simple.class)
	private List<String> tailoring;

	@JsonView(SessionView.Simple.class)
	private NavigableSet<SessionRequest> requests = new TreeSet<>();

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@JsonProperty("unique-id")
	public String getUniqueId() {
		return new StringBuilder().append(sessionId).append("_").append(startMicros).toString();
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public long getStartMicros() {
		return startMicros;
	}

	public void setStartMicros(long startMicros) {
		this.startMicros = startMicros;
	}

	public long getEndMicros() {
		return endMicros;
	}

	public void setEndMicros(long endMicros) {
		this.endMicros = endMicros;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public boolean isFresh() {
		return fresh;
	}

	public void setFresh(boolean fresh) {
		this.fresh = fresh;
	}

	public List<String> getTailoring() {
		return tailoring;
	}

	public void setTailoring(List<String> tailoring) {
		this.tailoring = tailoring;
	}

	public NavigableSet<SessionRequest> getRequests() {
		return requests;
	}

	public void addRequest(SessionRequest request) {
		if (this.requests == null) {
			this.requests = new TreeSet<>();
		}

		this.requests.add(request);
		this.startMicros = Math.min(this.startMicros, request.getStartMicros());
		this.endMicros = Math.max(this.endMicros, request.getEndMicros());
	}

	public void setRequests(NavigableSet<SessionRequest> requests) {
		this.requests = requests;
	}

	@JsonIgnore
	public String getTailoringAsString() {
		return convertTailoringToString(tailoring);
	}

	@JsonIgnore
	public String toSimpleLog() {
		return getUniqueId() + DELIM + requests.stream().map(SessionRequest::toSimpleLog).collect(Collectors.joining(DELIM));
	}

	@JsonIgnore
	public String toExtensiveLog() {
		return getUniqueId() + DELIM + requests.stream().map(SessionRequest::toExtensiveLog).collect(Collectors.joining(DELIM));
	}

	@Override
	public int hashCode() {
		return Objects.hash(sessionId, tailoring, version, startMicros);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if ((obj == null) || !(obj instanceof Session)) {
			return false;
		}

		Session other = (Session) obj;
		return (endMicros == other.endMicros) && (finished == other.finished) && Objects.equals(sessionId, other.sessionId) && Objects.equals(requests, other.requests)
				&& (startMicros == other.startMicros)
				&& Objects.equals(tailoring, other.tailoring) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(getUniqueId()).append(" (").append(startMicros).append(" - ").append(endMicros).append(")").toString();
	}

	public static String convertTailoringToString(List<String> tailoring) {
		if ((tailoring == null) || tailoring.isEmpty()) {
			return AppId.SERVICE_ALL;
		}

		Collections.sort(tailoring);

		return tailoring.stream().collect(Collectors.joining("."));
	}

	public static List<String> convertStringToTailoring(String tailoring) {
		List<String> list = Arrays.asList(tailoring.split("\\."));
		Collections.sort(list);

		return list;
	}

	public static class TailoringSerializer extends StdSerializer<List<String>> {

		private static final long serialVersionUID = -4973460573775064579L;

		@SuppressWarnings("unchecked")
		protected TailoringSerializer() {
			super((Class<List<String>>) (Class<?>) List.class);
		}

		@Override
		public void serialize(List<String> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(convertTailoringToString(value));
		}

	}

	public static class TailoringDeserializer extends StdDeserializer<List<String>> {

		private static final long serialVersionUID = -3619765155269279344L;

		protected TailoringDeserializer() {
			super(List.class);
		}

		@Override
		public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			return convertStringToTailoring(p.getValueAsString());
		}

	}

}
