package org.continuity.wessbas.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class MonitoringData {

	@JsonProperty("data")
	private String dataLink;

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
