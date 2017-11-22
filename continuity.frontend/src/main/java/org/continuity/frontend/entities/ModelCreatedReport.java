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

	@JsonProperty("workload-model-link")
	@JsonInclude(Include.NON_NULL)
	private String link;

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
		this.link = workloadModelLink;
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
	 * @param workloadModelLink
	 *            New value for {@link #link}
	 */
	public void setLink(String workloadModelLink) {
		this.link = workloadModelLink;
	}

}
