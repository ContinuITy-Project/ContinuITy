package org.continuity.api.entities.config;

import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class LoadTestConfiguration {

	@JsonProperty("workload-link")
	private String workloadModelLink;

	private AppId appId;

	@JsonProperty("num-users")
	private int numUsers;

	private long duration;

	private int rampup;

	/**
	 * Default constructor.
	 */
	public LoadTestConfiguration() {
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
	 * Gets {@link #appId}.
	 *
	 * @return {@link #appId}
	 */
	public AppId getAppId() {
		return this.appId;
	}

	/**
	 * Sets {@link #appId}.
	 *
	 * @param appId
	 *            New value for {@link #appId}
	 */
	public void setAppId(AppId appId) {
		this.appId = appId;
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
