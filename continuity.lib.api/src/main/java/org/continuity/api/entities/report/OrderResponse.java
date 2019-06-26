package org.continuity.api.entities.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "wait-link", "result-link", "num-reports" })
public class OrderResponse {

	@JsonProperty("wait-link")
	private String waitLink;

	@JsonProperty("result-link")
	private String resultLink;

	@JsonProperty("num-reports")
	private int numReports;

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

	public int getNumReports() {
		return numReports;
	}

	public void setNumReports(int numReports) {
		this.numReports = numReports;
	}

}
