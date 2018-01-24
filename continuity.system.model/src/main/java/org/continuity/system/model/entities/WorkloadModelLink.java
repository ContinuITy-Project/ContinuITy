package org.continuity.system.model.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelLink {

	@JsonProperty("system-model-link")
	private String systemModelLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public WorkloadModelLink() {
	}

	/**
	 * Gets {@link #systemModelLink}.
	 *
	 * @return {@link #systemModelLink}
	 */
	public String getSystemModelLink() {
		return this.systemModelLink;
	}

	/**
	 * Sets {@link #systemModelLink}.
	 *
	 * @param systemModelLink
	 *            New value for {@link #systemModelLink}
	 */
	public void setSystemModelLink(String systemModelLink) {
		this.systemModelLink = systemModelLink;
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
		return "{ tag: " + tag + ", system-model-link: " + systemModelLink + " }";
	}

}
