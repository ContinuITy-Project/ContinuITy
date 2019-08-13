package org.continuity.cobra.entities;

import java.util.List;
import java.util.Map;

import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.deserialization.TailoringDeserializer;
import org.continuity.api.entities.deserialization.TailoringSerializer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

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

	private double epsilon;

	@JsonProperty("min-sample-size")
	private long minSampleSize;

	@JsonProperty("start-micros")
	private long startMicros;

	@JsonProperty("interval-start-micros")
	private long intervalStartMicros;

	@JsonProperty("end-micros")
	private long endMicros;

	private List<String> states;

	@JsonProperty("previous-markov-chains")
	private Map<String, double[]> previousMarkovChains;

	private List<Session> sessions;

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

	public double getEpsilon() {
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

	public List<String> getStates() {
		return states;
	}

	public ClustinatorInput setStates(List<String> states) {
		this.states = states;
		return this;
	}

	public Map<String, double[]> getPreviousMarkovChains() {
		return previousMarkovChains;
	}

	public ClustinatorInput setPreviousMarkovChains(Map<String, double[]> previousMarkovChains) {
		this.previousMarkovChains = previousMarkovChains;
		return this;
	}

	public List<Session> getSessions() {
		return sessions;
	}

	public ClustinatorInput setSessions(List<Session> sessions) {
		this.sessions = sessions;
		return this;
	}

}
