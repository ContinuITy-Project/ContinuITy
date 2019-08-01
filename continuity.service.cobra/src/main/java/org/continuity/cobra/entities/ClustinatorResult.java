package org.continuity.cobra.entities;

import java.util.List;

import org.continuity.api.entities.deserialization.TailoringDeserializer;
import org.continuity.api.entities.deserialization.TailoringSerializer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClustinatorResult {

	@JsonProperty("app-id")
	private AppId appId;

	private VersionOrTimestamp version;

	@JsonSerialize(using = TailoringSerializer.class)
	@JsonDeserialize(using = TailoringDeserializer.class)
	private List<String> tailoring;

	@JsonProperty("start-micros")
	private long startMicros;

	@JsonProperty("interval-start-micros")
	private long intervalStartMicros;

	@JsonProperty("end-micros")
	private long endMicros;

	// TODO: clustering result

	public AppId getAppId() {
		return appId;
	}

	public void setAppId(AppId appId) {
		this.appId = appId;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public List<String> getTailoring() {
		return tailoring;
	}

	public void setTailoring(List<String> tailoring) {
		this.tailoring = tailoring;
	}

	public long getStartMicros() {
		return startMicros;
	}

	public void setStartMicros(long startMicros) {
		this.startMicros = startMicros;
	}

	public long getIntervalStartMicros() {
		return intervalStartMicros;
	}

	public void setIntervalStartMicros(long intervalStartMicros) {
		this.intervalStartMicros = intervalStartMicros;
	}

	public long getEndMicros() {
		return endMicros;
	}

	public void setEndMicros(long endMicros) {
		this.endMicros = endMicros;
	}

}
