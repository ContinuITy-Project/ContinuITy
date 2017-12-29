package org.continuity.jmeter.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class LoadTestSpecification {

	@JsonProperty("workload-link")
	private String workloadModelLink;

	private String tag;

	@JsonProperty("num-users")
	private int numUsers;

	private long duration;

	private int rampup;

	/**
	 * Default constructor.
	 */
	public LoadTestSpecification() {
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
	 * @param workloadModelId
	 *            New value for {@link #workloadModelLink}
	 */
	public void setWorkloadModelLink(String workloadModelId) {
		this.workloadModelLink = workloadModelId;
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
	 * Gets {@link #numUsers}.
	 * 
	 * @return {@link #numUsers}
	 */
	public int getNumUsers() {
		return this.numUsers;
	}

	/**
	 * Sets {@link #numUsers}.
	 * 
	 * @param numUsers
	 *            New value for {@link #numUsers}
	 */
	public void setNumUsers(int numUsers) {
		this.numUsers = numUsers;
	}

	/**
	 * Gets {@link #duration}.
	 * 
	 * @return {@link #duration}
	 */
	public long getDuration() {
		return this.duration;
	}

	/**
	 * Sets {@link #duration}.
	 * 
	 * @param duration
	 *            New value for {@link #duration}
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * Gets {@link #rampup}.
	 * 
	 * @return {@link #rampup}
	 */
	public int getRampup() {
		return this.rampup;
	}

	/**
	 * Sets {@link #rampup}.
	 * 
	 * @param rampup
	 *            New value for {@link #rampup}
	 */
	public void setRampup(int rampup) {
		this.rampup = rampup;
	}

}
