package org.continuity.frontend.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class ModelCreatedReport {

	private String message;

	@JsonProperty("link")
	@JsonInclude(Include.NON_NULL)
	private String workloadModelLink;

	/**
	 * Default constructor.
	 */
	public ModelCreatedReport() {
	}

	/**
	 * @param message
	 * @param workloadModelLink
	 */
	public ModelCreatedReport(String message, String workloadModelLink) {
		this.message = message;
		this.workloadModelLink = workloadModelLink;
	}

	/**
	 * @param message
	 */
	public ModelCreatedReport(String message) {
		this(message, null);
	}

	/**
	 * Gets {@link #message}.
	 *
	 * @return {@link #message}
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Sets {@link #message}.
	 *
	 * @param message
	 *            New value for {@link #message}
	 */
	public void setMessage(String message) {
		this.message = message;
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

}
