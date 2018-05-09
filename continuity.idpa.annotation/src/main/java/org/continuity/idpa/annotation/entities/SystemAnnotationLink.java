package org.continuity.idpa.annotation.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class SystemAnnotationLink {

	@JsonProperty("application-link")
	private String applicationLink;

	@JsonProperty("delta-link")
	private String deltaLink;

	@JsonProperty(value = "initial-annotation-link", required = false)
	private String annotationLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public SystemAnnotationLink() {
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
	 * Gets {@link #deltaLink}.
	 *
	 * @return {@link #deltaLink}
	 */
	public String getDeltaLink() {
		return this.deltaLink;
	}

	/**
	 * Sets {@link #deltaLink}.
	 *
	 * @param deltaLink
	 *            New value for {@link #deltaLink}
	 */
	public void setDeltaLink(String deltaLink) {
		this.deltaLink = deltaLink;
	}

	/**
	 * Gets {@link #annotationLink}.
	 *
	 * @return {@link #annotationLink}
	 */
	public String getAnnotationLink() {
		return this.annotationLink;
	}

	/**
	 * Sets {@link #annotationLink}.
	 *
	 * @param annotationLink
	 *            New value for {@link #annotationLink}
	 */
	public void setAnnotationLink(String annotationLink) {
		this.annotationLink = annotationLink;
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
		return "{  system-model-link: " + applicationLink + " }";
	}

}
