package org.continuity.wessbas.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack {

	private static final String DEFAULT_MODEL_TYPE = "wessbas";

	@JsonProperty("model-type")
	private String modelType = DEFAULT_MODEL_TYPE;

	@JsonProperty("workload-link")
	private String workloadLink;

	@JsonProperty("system-model-link")
	private String systemModelLink;

	@JsonProperty("initial-annotation-link")
	private String initialAnnotationLink;

	private String tag;

	/**
	 * Default constructor.
	 */
	public WorkloadModelPack() {
	}

	public WorkloadModelPack(String hostname, String id, String tag) {
		String base = "http://" + hostname + "/model/" + id;
		this.workloadLink = base + "/workload";
		this.systemModelLink = base + "/system";
		this.initialAnnotationLink = base + "/annotation";
		this.tag = tag;
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
	 * @param workloadLink
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
	 * @param systemModel
	 *            New value for {@link #systemModelLink}
	 */
	public void setSystemModelLink(String systemModelLink) {
		this.systemModelLink = systemModelLink;
	}

	/**
	 * Gets {@link #initialAnnotationLink}.
	 *
	 * @return {@link #initialAnnotation}
	 */
	public String getInitialAnnotationLink() {
		return this.initialAnnotationLink;
	}

	/**
	 * Sets {@link #initialAnnotationLink}.
	 *
	 * @param initialAnnotation
	 *            New value for {@link #initialAnnotationLink}
	 */
	public void setInitialAnnotationLink(String initialAnnotationLink) {
		this.initialAnnotationLink = initialAnnotationLink;
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
		return "{ type: " + modelType + ", tag: " + tag + ", workload-link: " + workloadLink + " }";
	}

}
