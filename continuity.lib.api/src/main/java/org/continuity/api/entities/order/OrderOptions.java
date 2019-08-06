package org.continuity.api.entities.order;

import java.util.HashMap;
import java.util.Map;

import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.api.rest.RestApi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "num-users", "duration", "rampup", "tailoring-approach", "forecast-approach", "producers" })
public class OrderOptions {

	@JsonProperty("num-users")
	@JsonInclude(Include.NON_NULL)
	private Integer numUsers;

	@JsonInclude(Include.NON_NULL)
	private Long duration;

	@JsonInclude(Include.NON_NULL)
	private Integer rampup;

	@JsonProperty("tailoring-approach")
	@JsonInclude(Include.NON_NULL)
	private TailoringApproach tailoringApproach;

	@JsonProperty("forecast-approach")
	@JsonInclude(Include.NON_NULL)
	private String forecastApproach;

	@JsonInclude(Include.NON_NULL)
	private Map<ArtifactType, String> producers;

	public Integer getNumUsers() {
		return numUsers;
	}

	public void setNumUsers(int numUsers) {
		this.numUsers = numUsers;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Integer getRampup() {
		return rampup;
	}

	public void setRampup(int rampup) {
		this.rampup = rampup;
	}

	public TailoringApproach getTailoringApproach() {
		return tailoringApproach;
	}

	@JsonIgnore
	public TailoringApproach getTailoringApproachOrDefault() {
		return tailoringApproach == null ? TailoringApproach.LOG_BASED : tailoringApproach;
	}

	public void setTailoringApproach(TailoringApproach tailoringApproach) {
		this.tailoringApproach = tailoringApproach;
	}

	public String getForecastApproach() {
		return forecastApproach;
	}

	@JsonIgnore
	public String getForecastApproachOrDefault() {
		return forecastApproach == null ? "Telescope" : forecastApproach;
	}

	public void setForecastApproach(String forecastApproach) {
		this.forecastApproach = forecastApproach;
	}

	public Map<ArtifactType, String> getProducers() {
		return producers;
	}

	public void setProducers(Map<ArtifactType, String> producers) {
		this.producers = producers;
	}

	@JsonIgnore
	public Map<ArtifactType, String> getProducersOrDefault() {
		Map<ArtifactType, String> allProducers = new HashMap<>();

		if (producers != null) {
			allProducers.putAll(producers);
		}

		allProducers.putIfAbsent(ArtifactType.TRACES, RestApi.Cobra.SERVICE_NAME);
		allProducers.putIfAbsent(ArtifactType.SESSIONS, RestApi.Cobra.SERVICE_NAME);
		allProducers.putIfAbsent(ArtifactType.BEHAVIOR_MODEL, RestApi.Cobra.SERVICE_NAME);
		allProducers.putIfAbsent(ArtifactType.WORKLOAD_MODEL, RestApi.Wessbas.SERVICE_NAME);
		allProducers.putIfAbsent(ArtifactType.LOAD_TEST, RestApi.JMeter.SERVICE_NAME);
		allProducers.putIfAbsent(ArtifactType.TEST_RESULT, RestApi.JMeter.SERVICE_NAME);

		return allProducers;
	}

}
