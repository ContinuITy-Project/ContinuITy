package org.continuity.wessbas.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import m4jdsl.WorkloadModel;

public class WessbasBundle {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH-mm-ss-SSSX")
	private Date timestamp;

	@JsonIgnore
	private WorkloadModel workloadModel;

	public WessbasBundle(Date timestamp, WorkloadModel workloadModel) {
		this.timestamp = timestamp;
		this.workloadModel = workloadModel;
	}

	public WessbasBundle() {
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public WorkloadModel getWorkloadModel() {
		return workloadModel;
	}

	public void setWorkloadModel(WorkloadModel workloadModel) {
		this.workloadModel = workloadModel;
	}

}
