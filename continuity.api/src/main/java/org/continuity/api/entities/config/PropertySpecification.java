package org.continuity.api.entities.config;

import org.continuity.dsl.description.IntensityCalculationInterval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertySpecification {

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

	public Integer getNumUsers() {
		return numUsers;
	}

	public PropertySpecification setNumUsers(Integer numUsers) {
		this.numUsers = numUsers;
		return this;
	}

	public Long getDuration() {
		return duration;
	}

	public PropertySpecification setDuration(Long duration) {
		this.duration = duration;
		return this;
	}

	public Integer getRampup() {
		return rampup;
	}

	public PropertySpecification setRampup(Integer rampup) {
		this.rampup = rampup;
		return this;
	}

	public IntensityCalculationInterval getIntensityCalculationInterval() {
		return intensityCalculationInterval;
	}

	public void setIntensityCalculationInterval(IntensityCalculationInterval intensityCalculationInterval) {
		this.intensityCalculationInterval = intensityCalculationInterval;
	}

}
