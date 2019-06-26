package org.continuity.api.entities.config;

import org.continuity.dsl.description.IntensityCalculationInterval;

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
	
	@JsonProperty("intensity-calculation-interval")
	@JsonInclude(Include.NON_NULL)
	private IntensityCalculationInterval intensityCalculationInterval;

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

	public IntensityCalculationInterval getIntensityCalculationInterval() {
		return intensityCalculationInterval;
	}

	public void setIntensityCalculationInterval(IntensityCalculationInterval intensityCalculationInterval) {
		this.intensityCalculationInterval = intensityCalculationInterval;
	}

	public PropertySpecification toProperties() {
		PropertySpecification props = new PropertySpecification();

		props.setDuration(duration);
		props.setNumUsers(numUsers);
		props.setRampup(rampup);
		props.setIntensityCalculationInterval(intensityCalculationInterval);

		return props;
	}

}
