package org.continuity.frontend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelInput {

	@JsonProperty("data")
	private String monitoringDataLink;

	@JsonProperty("reserved")
	private String storageLink;

	public WorkloadModelInput(String monitoringDataLink, String storageLink) {
		this.monitoringDataLink = monitoringDataLink;
		this.storageLink = storageLink;
	}

	public WorkloadModelInput() {
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
	 * Gets {@link #storageLink}.
	 *
	 * @return {@link #storageLink}
	 */
	public String getStorageLink() {
		return this.storageLink;
	}

	/**
	 * Sets {@link #storageLink}.
	 *
	 * @param storageLink
	 *            New value for {@link #storageLink}
	 */
	public void setStorageLink(String storageLink) {
		this.storageLink = storageLink;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ data: \"" + monitoringDataLink + "\", reserved: \"" + storageLink + "\" }";
	}

}
