package org.continuity.wessbas.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class MonitoringData {

	@JsonProperty("data")
	private String link;

	private String tag;

	/**
	 * Gets {@link #link}.
	 *
	 * @return {@link #link}
	 */
	public String getLink() {
		return this.link;
	}

	/**
	 * Sets {@link #link}.
	 *
	 * @param link
	 *            New value for {@link #link}
	 */
	public void setLink(String link) {
		this.link = link;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ link: \"" + link + "\", tag: \"" + tag + "\" }";
	}

}
