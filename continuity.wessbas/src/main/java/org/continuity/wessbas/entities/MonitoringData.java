package org.continuity.wessbas.entities;

import java.util.Date;

import org.continuity.commons.format.CommonFormats;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class MonitoringData {

	@JsonProperty("data")
	private String dataLink;

	@JsonProperty(value = "timestamp", required = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = CommonFormats.DATE_FORMAT)
	private Date timestamp = new Date();

	@JsonProperty("reserved")
	private String storageLink;

	/**
	 * Gets {@link #dataLink}.
	 *
	 * @return {@link #dataLink}
	 */
	public String getDataLink() {
		return this.dataLink;
	}

	/**
	 * Sets {@link #dataLink}.
	 *
	 * @param dataLink
	 *            New value for {@link #dataLink}
	 */
	public void setDataLink(String dataLink) {
		this.dataLink = dataLink;
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
		return "{ data: \"" + dataLink + "\", reserved: \"" + storageLink + "\" }";
	}

}
