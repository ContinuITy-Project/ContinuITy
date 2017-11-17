package org.continuity.wessbas.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class LoadTestSpecification {

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
	 * Constructor.
	 *
	 * @param workloadModelLink
	 *            Link to the workload model.
	 * @param annotationLink
	 *            Link to the annotation model.
	 */
	public LoadTestSpecification(String workloadModelLink, String annotationLink) {
		super();
		this.workloadModelLink = workloadModelLink;
		this.annotationLink = annotationLink;
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
