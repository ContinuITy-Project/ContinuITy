package org.continuity.frontend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class LoadTestSpecification {

	@JsonProperty("workload-type")
	private String workloadModelType;

	@JsonProperty("load-test-type")
	private String loadTestType;

	@JsonProperty("workload-link")
	private String workloadModelLink;

	@JsonProperty("annotation-link")
	private String annotationLink;

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
	 * Gets {@link #loadTestType}.
	 * 
	 * @return {@link #loadTestType}
	 */
	public String getLoadTestType() {
		return this.loadTestType;
	}

	/**
	 * Sets {@link #loadTestType}.
	 * 
	 * @param loadTestType
	 *            New value for {@link #loadTestType}
	 */
	public void setLoadTestType(String loadTestType) {
		this.loadTestType = loadTestType;
	}

	/**
	 * Gets {@link #workloadModelLink}.
	 *
	 * @return {@link #workloadModelLink}
	 */
	public String getWorkloadModelLink() {
		return this.workloadModelLink;
	}

	/**
	 * Sets {@link #workloadModelLink}.
	 *
	 * @param workloadModelLink
	 *            New value for {@link #workloadModelLink}
	 */
	public void setWorkloadModelLink(String workloadModelLink) {
		this.workloadModelLink = workloadModelLink;
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

}
