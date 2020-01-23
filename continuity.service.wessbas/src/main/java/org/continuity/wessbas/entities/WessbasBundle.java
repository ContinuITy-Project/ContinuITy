package org.continuity.wessbas.entities;

import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import m4jdsl.WorkloadModel;

public class WessbasBundle {

	private VersionOrTimestamp version;

	@JsonIgnore
	private WorkloadModel workloadModel;

	@JsonInclude(Include.NON_NULL)
	private String intensities;

	@JsonInclude(Include.NON_NULL)
	private Integer intensityResolution;

	public WessbasBundle(VersionOrTimestamp version, WorkloadModel workloadModel, String intensities, Integer intensityResolution) {
		this.version = version;
		this.workloadModel = workloadModel;
		this.intensities = intensities;
		this.intensityResolution = intensityResolution;
	}

	public WessbasBundle(VersionOrTimestamp version, WorkloadModel workloadModel) {
		this(version, workloadModel, null, null);
	}

	public WessbasBundle() {
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public WorkloadModel getWorkloadModel() {
		return workloadModel;
	}

	public void setWorkloadModel(WorkloadModel workloadModel) {
		this.workloadModel = workloadModel;
	}

	public String getIntensities() {
		return intensities;
	}

	public void setIntensities(String intensities) {
		this.intensities = intensities;
	}

	public Integer getIntensityResolution() {
		return intensityResolution;
	}

	public void setIntensityResolution(Integer intensityResolution) {
		this.intensityResolution = intensityResolution;
	}

}
