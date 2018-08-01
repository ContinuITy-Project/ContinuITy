package org.continuity.api.entities.report;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderReport {

	private String orderId;

	@JsonProperty("created-artifacts")
	private LinkExchangeModel createdArtifacts;

	@JsonProperty("internal-artifacts")
	private LinkExchangeModel internalArtifacts;

	private boolean successful;

	@JsonInclude(Include.NON_NULL)
	private String error;

	public OrderReport() {
	}

	public OrderReport(String orderId, LinkExchangeModel internalArtifacts, boolean successful, String error) {
		this.orderId = orderId;
		this.internalArtifacts = internalArtifacts;
		this.successful = successful;
		this.error = error;
	}

	public static OrderReport asSuccessful(String orderId, LinkExchangeModel internalArtifacts) {
		return new OrderReport(orderId, internalArtifacts, true, null);
	}

	public static OrderReport asError(String orderId, LinkExchangeModel internalArtifacts, String error) {
		return new OrderReport(orderId, internalArtifacts, false, error);
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public LinkExchangeModel getCreatedArtifacts() {
		return createdArtifacts;
	}

	public void setCreatedArtifacts(LinkExchangeModel createdArtifacts) {
		this.createdArtifacts = createdArtifacts;
	}

	public LinkExchangeModel getInternalArtifacts() {
		return internalArtifacts;
	}

	public void setInternalArtifacts(LinkExchangeModel internalArtifacts) {
		this.internalArtifacts = internalArtifacts;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
