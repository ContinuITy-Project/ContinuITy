package org.continuity.frontend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelConfig {

	@JsonProperty("type")
	private String workloadModelType;

	@JsonProperty("data")
	private String monitoringDataLink;

	/**
	 * Default constructor.
	 */
	public WorkloadModelConfig() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param workloadModelType
	 * @param monitoringDataLink
	 */
	public WorkloadModelConfig(String workloadModelType, String monitoringDataLink) {
		super();
		this.workloadModelType = workloadModelType;
		this.monitoringDataLink = monitoringDataLink;
	}

	/**
	 * Gets {@link #workloadModelType}.
	 * 
	 * @return {@link #workloadModelType}
	 */
	public String getWorkloadModelType() {
		return this.workloadModelType;
	}

	/**
	 * Sets {@link #workloadModelType}.
	 * 
	 * @param workloadModelType
	 *            New value for {@link #workloadModelType}
	 */
	public void setWorkloadModelType(String workloadModelType) {
		this.workloadModelType = workloadModelType;
	}

	/**
	 * Gets {@link #monitoringDataLink}.
	 * 
	 * @return {@link #monitoringDataLink}
	 */
	public String getMonitoringDataLink() {
		return this.monitoringDataLink;
	}

	/**
	 * Sets {@link #monitoringDataLink}.
	 * 
	 * @param monitoringDataLink
	 *            New value for {@link #monitoringDataLink}
	 */
	public void setMonitoringDataLink(String monitoringDataLink) {
		this.monitoringDataLink = monitoringDataLink;
	}

}
