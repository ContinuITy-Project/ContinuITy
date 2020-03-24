package org.continuity.cobra.entities;

import java.util.List;
import java.util.Map;

import org.continuity.api.entities.config.cobra.AppendStrategy;
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

	@JsonProperty("append-strategy")
	private AppendStrategy appendStrategy;

	@JsonProperty("end-micros")
	private long endMicros;

	private List<String> states;

	@JsonProperty("mean-markov-chains")
	private Map<String, double[]> meanMarkovChains;

	@JsonProperty("think-time-means")
	private Map<String, double[]> thinkTimeMeans;

	@JsonProperty("think-time-variances")
	private Map<String, double[]> thinkTimeVariances;

	private Map<String, Double> frequency;

	@JsonProperty("num-sessions")
	private Map<String, Long> numSessions;

	private Map<String, double[]> radiuses;

	private ClusteringContinuation continuation;

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

	public AppendStrategy getAppendStrategy() {
		return appendStrategy;
	}

	public void setAppendStrategy(AppendStrategy appendStrategy) {
		this.appendStrategy = appendStrategy;
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

	public List<String> getStates() {
		return states;
	}

	public void setStates(List<String> states) {
		this.states = states;
	}

	public Map<String, double[]> getMeanMarkovChains() {
		return meanMarkovChains;
	}

	public void setMeanMarkovChains(Map<String, double[]> meanMarkovChains) {
		this.meanMarkovChains = meanMarkovChains;
	}

	public Map<String, double[]> getThinkTimeMeans() {
		return thinkTimeMeans;
	}

	public void setThinkTimeMeans(Map<String, double[]> thinkTimeMeans) {
		this.thinkTimeMeans = thinkTimeMeans;
	}

	public Map<String, double[]> getThinkTimeVariances() {
		return thinkTimeVariances;
	}

	public void setThinkTimeVariances(Map<String, double[]> thinkTimeVariances) {
		this.thinkTimeVariances = thinkTimeVariances;
	}

	public Map<String, Double> getFrequency() {
		return frequency;
	}

	public void setFrequency(Map<String, Double> frequency) {
		this.frequency = frequency;
	}

	public Map<String, Long> getNumSessions() {
		return numSessions;
	}

	public void setNumSessions(Map<String, Long> numSessions) {
		this.numSessions = numSessions;
	}

	public ClusteringContinuation getContinuation() {
		return continuation;
	}

	public void setContinuation(ClusteringContinuation continuation) {
		this.continuation = continuation;
	}

	public Map<String, double[]> getRadiuses() {
		return radiuses;
	}

	public void setRadiuses(Map<String, double[]> radiuses) {
		this.radiuses = radiuses;
	}

}
