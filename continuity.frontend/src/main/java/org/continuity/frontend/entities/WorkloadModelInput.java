package org.continuity.frontend.entities;

import java.util.Date;

import org.continuity.commons.format.CommonFormats;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelInput {

	@JsonProperty("data")
	private String monitoringDataLink;

	@JsonProperty(value = "timestamp", required = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = CommonFormats.DATE_FORMAT)
	private Date timestamp = new Date();

	@JsonProperty("reserved")
	private String storageLink;

	public WorkloadModelInput(String monitoringDataLink, Date timestamp, String storageLink) {
		this.monitoringDataLink = monitoringDataLink;
		this.storageLink = storageLink;
		this.timestamp = timestamp;
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
	 * Gets {@link #timestamp}.
	 * 
	 * @return {@link #timestamp}
	 */
	public Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets {@link #timestamp}.
	 * 
	 * @param timestamp
	 *            New value for {@link #timestamp}
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ data: \"" + monitoringDataLink + "\", reserved: \"" + storageLink + "\" }";
	}

}
