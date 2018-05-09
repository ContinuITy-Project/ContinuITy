package org.continuity.idpa.application.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelLink {

	@JsonProperty("application-link")
	private String applicationLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public WorkloadModelLink() {
	}

	/**
	 * Gets {@link #applicationLink}.
	 *
	 * @return {@link #applicationLink}
	 */
	public String getApplicationLink() {
		return this.applicationLink;
	}

	/**
	 * Sets {@link #applicationLink}.
	 *
	 * @param applicationLink
	 *            New value for {@link #applicationLink}
	 */
	public void setApplicationLink(String applicationLink) {
		this.applicationLink = applicationLink;
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
		return "{ tag: " + tag + ", system-model-link: " + applicationLink + " }";
	}

}
