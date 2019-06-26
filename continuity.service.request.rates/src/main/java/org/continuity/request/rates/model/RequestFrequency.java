package org.continuity.request.rates.model;

import org.continuity.idpa.application.Endpoint;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "freq", "endpoint" })
public class RequestFrequency {

	private double freq;

	private Endpoint<?> endpoint;

	public RequestFrequency() {
	}

	public RequestFrequency(double freq, Endpoint<?> endpoint) {
		this.freq = freq;
		this.endpoint = endpoint;
	}

	public double getFreq() {
		return freq;
	}

	public void setFreq(double freq) {
		this.freq = freq;
	}

	public Endpoint<?> getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint<?> endpoint) {
		this.endpoint = endpoint;
	}

}
