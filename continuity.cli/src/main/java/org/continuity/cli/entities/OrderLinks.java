package org.continuity.cli.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Henning Schulz
 *
 */
public class OrderLinks {

	@JsonProperty("wait-link")
	private String waitLink;

	@JsonProperty("result-link")
	private String resultLink;

	public String getWaitLink() {
		return waitLink;
	}

	public void setWaitLink(String waitLink) {
		this.waitLink = waitLink;
	}

	public String getResultLink() {
		return resultLink;
	}

	public void setResultLink(String resultLink) {
		this.resultLink = resultLink;
	}

}
