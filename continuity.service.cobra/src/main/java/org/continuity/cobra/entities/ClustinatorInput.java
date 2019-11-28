package org.continuity.cobra.entities;

import java.util.List;

import org.continuity.api.entities.deserialization.TailoringDeserializer;
import org.continuity.api.entities.deserialization.TailoringSerializer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "app-id", "version", "tailoring", "epsilon", "min-sample-size", "start-micros", "interval-start-micros", "end-micros", "states", "previous-markov-chains", "sessions" })
public class ClustinatorInput {

	@JsonProperty("app-id")
	private AppId appId;

	private VersionOrTimestamp version;

	@JsonSerialize(using = TailoringSerializer.class)
	@JsonDeserialize(using = TailoringDeserializer.class)
	private List<String> tailoring;

	@JsonProperty("avg-transition-tolerance")
	@JsonInclude(Include.NON_NULL)
	private Double avgTransitionTolerance;

	@JsonInclude(Include.NON_NULL)
	private Double epsilon;

	@JsonProperty("min-sample-size")
	private long minSampleSize;

	@JsonProperty("start-micros")
	private long startMicros;

	@JsonProperty("interval-start-micros")
	private long intervalStartMicros;

	@JsonProperty("end-micros")
	private long endMicros;

	private long lookback;

	public AppId getAppId() {
		return appId;
	}

	public ClustinatorInput setAppId(AppId appId) {
		this.appId = appId;
		return this;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public ClustinatorInput setVersion(VersionOrTimestamp version) {
		this.version = version;
		return this;
	}

	public List<String> getTailoring() {
		return tailoring;
	}

	public ClustinatorInput setTailoring(List<String> tailoring) {
		this.tailoring = tailoring;
		return this;
	}

	public Double getAvgTransitionTolerance() {
		return avgTransitionTolerance;
	}

	public ClustinatorInput setAvgTransitionTolerance(double avgTransitionTolerance) {
		this.avgTransitionTolerance = avgTransitionTolerance;
		return this;
	}

	public Double getEpsilon() {
		return epsilon;
	}

	public ClustinatorInput setEpsilon(double epsilon) {
		this.epsilon = epsilon;
		return this;
	}

	public long getMinSampleSize() {
		return minSampleSize;
	}

	public ClustinatorInput setMinSampleSize(long minSampleSize) {
		this.minSampleSize = minSampleSize;
		return this;
	}

	public long getStartMicros() {
		return startMicros;
	}

	public ClustinatorInput setStartMicros(long startMicros) {
		this.startMicros = startMicros;
		return this;
	}

	public long getIntervalStartMicros() {
		return intervalStartMicros;
	}

	public ClustinatorInput setIntervalStartMicros(long intervalStartMicros) {
		this.intervalStartMicros = intervalStartMicros;
		return this;
	}

	public long getEndMicros() {
		return endMicros;
	}

	public ClustinatorInput setEndMicros(long endMicros) {
		this.endMicros = endMicros;
		return this;
	}

	public long getLookback() {
		return lookback;
	}

	public ClustinatorInput setLookback(long lookback) {
		this.lookback = lookback;
		return this;
	}

}
