package org.continuity.wessbas.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack {

	private static final String DEFAULT_MODEL_TYPE = "wessbas";

	private static final String ERROR_LINK = "INVALID";

	@JsonProperty("model-type")
	private String modelType = DEFAULT_MODEL_TYPE;

	@JsonProperty("workload-link")
	private String workloadLink;

	@JsonProperty("system-model-link")
	private String systemModelLink;

	@JsonProperty("initial-annotation-link")
	private String initialAnnotationLink;

	@JsonProperty("jmeter-link")
	private String jmeterLink;

	private String tag;

	private boolean error;

	/**
	 * Default constructor.
	 */
	public WorkloadModelPack() {
	}

	public WorkloadModelPack(String hostname, String id, String tag) {
		String base = hostname + "/model/" + id;
		this.workloadLink = base + "/workload";
		this.systemModelLink = base + "/system";
		this.initialAnnotationLink = base + "/annotation";
		this.jmeterLink = hostname + "/loadtest/jmeter/" + id + "/create";
		this.tag = tag;
		this.error = false;
	}

	public static WorkloadModelPack asError(String hostname, String id, String tag) {
		WorkloadModelPack pack = new WorkloadModelPack(hostname, id, tag);
		pack.setSystemModelLink(ERROR_LINK);
		pack.setInitialAnnotationLink(ERROR_LINK);
		pack.setJmeterLink(ERROR_LINK);
		pack.setError(true);

		return pack;
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
	 * Gets {@link #jmeterLink}.
	 *
	 * @return {@link #jmeterLink}
	 */
	public String getJmeterLink() {
		return this.jmeterLink;
	}

	/**
	 * Sets {@link #jmeterLink}.
	 *
	 * @param jmeterLink
	 *            New value for {@link #jmeterLink}
	 */
	public void setJmeterLink(String jmeterLink) {
		this.jmeterLink = jmeterLink;
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
	 * Gets {@link #error}.
	 *
	 * @return {@link #error}
	 */
	public boolean isError() {
		return this.error;
	}

	/**
	 * Sets {@link #error}.
	 *
	 * @param error
	 *            New value for {@link #error}
	 */
	public void setError(boolean error) {
		this.error = error;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ error: " + error + ", type: " + modelType + ", tag: " + tag + ", workload-link: " + workloadLink + " }";
	}

}
