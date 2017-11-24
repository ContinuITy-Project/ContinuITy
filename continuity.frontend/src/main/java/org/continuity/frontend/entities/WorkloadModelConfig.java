package org.continuity.frontend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelConfig {

	@JsonProperty("data")
	private String monitoringDataLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public WorkloadModelConfig() {
		// TODO Auto-generated constructor stub
	}

	public WorkloadModelConfig(String monitoringDataLink, String tag) {
		super();
		this.monitoringDataLink = monitoringDataLink;
		this.tag = tag;
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

	/**
	 * Gets {@link #tag}.
	 *
	 * @return {@link #tag}
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * Sets {@link #tag}.
	 *
	 * @param tag
	 *            New value for {@link #tag}
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

}
