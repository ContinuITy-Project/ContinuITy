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

	@JsonProperty("workload-link")
	private String workloadLink;

	@JsonProperty("system-model-link")
	private String systemModelLink;

	@JsonProperty(value = "initial-annotation-link", required = false)
	private String annotationLink;

	@JsonProperty(value = "custom-annotation-link", required = false)
	private String customAnnotationLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public WorkloadModelLink() {
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
	 * Gets {@link #workloadLink}.
	 *
	 * @return {@link #workloadLink}
	 */
	public String getWorkloadLink() {
		return this.workloadLink;
	}

	/**
	 * Sets {@link #workloadLink}.
	 *
	 * @param link
	 *            New value for {@link #workloadLink}
	 */
	public void setWorkloadLink(String workloadLink) {
		this.workloadLink = workloadLink;
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
		return "{ type: " + modelType + ", link: " + workloadLink + ", system-model-link: " + systemModelLink + " }";
	}

}
