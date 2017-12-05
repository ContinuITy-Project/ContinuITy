package org.continuity.frontend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class LoadTestSpecification {

	@JsonProperty("workload-type")
	private String workloadModelType;

	@JsonProperty("workload-id")
	private String workloadModelId;

	private String tag;

	/**
	 * Default constructor.
	 */
	public LoadTestSpecification() {
	}

	/**
	 * Gets {@link #workloadModelType}.
	 *
	 * @return {@link #workloadModelType}
	 */
	public String getWorkloadModelType() {
		return this.workloadModelType;
	}

	/**
	 * Sets {@link #workloadModelType}.
	 *
	 * @param workloadModelType
	 *            New value for {@link #workloadModelType}
	 */
	public void setWorkloadModelType(String workloadModelType) {
		this.workloadModelType = workloadModelType;
	}

	/**
	 * Gets {@link #workloadModelId}.
	 *
	 * @return {@link #workloadModelId}
	 */
	public String getWorkloadModelId() {
		return this.workloadModelId;
	}

	/**
	 * Sets {@link #workloadModelId}.
	 *
	 * @param workloadModelId
	 *            New value for {@link #workloadModelId}
	 */
	public void setWorkloadModelId(String workloadModelId) {
		this.workloadModelId = workloadModelId;
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

}
