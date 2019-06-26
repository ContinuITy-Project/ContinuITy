package org.continuity.request.rates.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A simple workload model holding a request rate per minute and a frequency of called endpoints.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "requests-per-minute", "mix" })
public class RequestRatesModel {

	@JsonProperty("requests-per-minute")
	private double requestsPerMinute;

	private List<RequestFrequency> mix;

	public double getRequestsPerMinute() {
		return requestsPerMinute;
	}

	public void setRequestsPerMinute(double requestPerMinute) {
		this.requestsPerMinute = requestPerMinute;
	}

	public List<RequestFrequency> getMix() {
		return mix;
	}

	public void setMix(List<RequestFrequency> mix) {
		this.mix = mix;
	}

}
