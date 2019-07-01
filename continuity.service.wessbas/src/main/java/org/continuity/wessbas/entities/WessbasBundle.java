package org.continuity.wessbas.entities;

import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import m4jdsl.WorkloadModel;

public class WessbasBundle {

	private VersionOrTimestamp version;

	@JsonIgnore
	private WorkloadModel workloadModel;

	public WessbasBundle(VersionOrTimestamp version, WorkloadModel workloadModel) {
		this.version = version;
		this.workloadModel = workloadModel;
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

}
