package org.continuity.system.annotation.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class SystemAnnotationLink {

	@JsonProperty("system-model-link")
	private String systemModelLink;

	@JsonProperty("delta-link")
	private String deltaLink;

	@JsonProperty(value = "initial-annotation-link", required = false)
	private String annotationLink;

	@JsonProperty(value = "custom-annotation-link", required = false)
	private String customAnnotationLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public SystemAnnotationLink() {
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
	 * Gets {@link #customAnnotationLink}.
	 *
	 * @return {@link #customAnnotationLink}
	 */
	public String getCustomAnnotationLink() {
		return this.customAnnotationLink;
	}

	/**
	 * Sets {@link #customAnnotationLink}.
	 *
	 * @param extendedAnnotationLink
	 *            New value for {@link #customAnnotationLink}
	 */
	public void setCustomAnnotationLink(String customAnnotationLink) {
		this.customAnnotationLink = customAnnotationLink;
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
		return "{  system-model-link: " + systemModelLink + " }";
	}

}
