package org.continuity.wessbas.entities;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;

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

	@JsonProperty("system-model")
	private SystemModel systemModel;

	@JsonProperty("initial-annotation")
	private SystemAnnotation initialAnnotation;

	/**
	 * Default constructor.
	 */
	public WorkloadModelPack() {
	}

	public WorkloadModelPack(String link) {
		this.workloadLink = link;
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
	public String getLink() {
		return this.workloadLink;
	}

	/**
	 * Sets {@link #workloadLink}.
	 *
	 * @param link
	 *            New value for {@link #workloadLink}
	 */
	public void setLink(String link) {
		this.workloadLink = link;
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
	 * Gets {@link #systemModel}.
	 * 
	 * @return {@link #systemModel}
	 */
	public SystemModel getSystemModel() {
		return this.systemModel;
	}

	/**
	 * Sets {@link #systemModel}.
	 * 
	 * @param systemModel
	 *            New value for {@link #systemModel}
	 */
	public void setSystemModel(SystemModel systemModel) {
		this.systemModel = systemModel;
	}

	/**
	 * Gets {@link #initialAnnotation}.
	 * 
	 * @return {@link #initialAnnotation}
	 */
	public SystemAnnotation getInitialAnnotation() {
		return this.initialAnnotation;
	}

	/**
	 * Sets {@link #initialAnnotation}.
	 * 
	 * @param initialAnnotation
	 *            New value for {@link #initialAnnotation}
	 */
	public void setInitialAnnotation(SystemAnnotation initialAnnotation) {
		this.initialAnnotation = initialAnnotation;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ type: " + modelType + ", link: " + workloadLink + " }";
	}

}
