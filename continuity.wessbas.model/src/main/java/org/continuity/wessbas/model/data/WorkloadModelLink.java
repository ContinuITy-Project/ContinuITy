package org.continuity.wessbas.model.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @author Henning Schulz
 *
 */
@JsonTypeName("workload-model-instance")
public class WorkloadModelLink {

	private static final String DEFAULT_MODEL_TYPE = "wessbas";

	@JsonProperty("model-type")
	private String modelType = DEFAULT_MODEL_TYPE;

	private String link;

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
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "{ type: " + modelType + ", link: " + link + " }";
	}

}
