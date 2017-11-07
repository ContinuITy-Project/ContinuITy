package org.continuity.workload.annotation.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelLink {

	private static final String DEFAULT_MODEL_TYPE = "wessbas";

	@JsonProperty("model-type")
	private String modelType = DEFAULT_MODEL_TYPE;

	private String link;

	@JsonProperty("system-model-link")
	private String systemModelLink;

	@JsonProperty(value = "annotation-link", required = false)
	private String annotationLink;

	@JsonProperty(value = "extended-annotation-link", required = false)
	private String extendedAnnotationLink;

	/**
	 * Default constructor.
	 */
	public WorkloadModelLink() {
	}

	public WorkloadModelLink(String link) {
		this.link = link;
	}

	/**
	 * Gets {@link #modelType}.
	 *
	 * @return {@link #modelType}
	 */
	public String getModelType() {
		return this.modelType;
	}

	/**
	 * Sets {@link #modelType}.
	 *
	 * @param modelType
	 *            New value for {@link #modelType}
	 */
	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

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
	 * Gets {@link #extendedAnnotationLink}.
	 *
	 * @return {@link #extendedAnnotationLink}
	 */
	public String getExtendedAnnotationLink() {
		return this.extendedAnnotationLink;
	}

	/**
	 * Sets {@link #extendedAnnotationLink}.
	 *
	 * @param extendedAnnotationLink
	 *            New value for {@link #extendedAnnotationLink}
	 */
	public void setExtendedAnnotationLink(String extendedAnnotationLink) {
		this.extendedAnnotationLink = extendedAnnotationLink;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ type: " + modelType + ", link: " + link + ", system-model-link: " + systemModelLink + " }";
	}

}
