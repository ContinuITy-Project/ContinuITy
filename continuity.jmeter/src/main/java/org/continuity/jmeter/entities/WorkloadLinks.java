package org.continuity.jmeter.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a workload model including the links for getting the raw workload model and a JMeter
 * load test.
 *
 * @author Henning Schulz
 *
 */
public class WorkloadLinks {

	@JsonProperty("workload-link")
	private String workloadLink;

	@JsonProperty("jmeter-link")
	private String jmeterLink;

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

}
