package org.continuity.api.entities.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderOptions {

	@JsonProperty("workload-model-type")
	@JsonInclude(Include.NON_NULL)
	private WorkloadModelType workloadModelType;

	@JsonProperty("load-test-type")
	@JsonInclude(Include.NON_NULL)
	private LoadTestType loadTestType;

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

	public WorkloadModelType getWorkloadModelType() {
		return workloadModelType;
	}

	public void setWorkloadModelType(WorkloadModelType workloadModelType) {
		this.workloadModelType = workloadModelType;
	}

	public LoadTestType getLoadTestType() {
		return loadTestType;
	}

	public void setLoadTestType(LoadTestType loadTestType) {
		this.loadTestType = loadTestType;
	}

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

}
