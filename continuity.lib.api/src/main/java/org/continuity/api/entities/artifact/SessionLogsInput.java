package org.continuity.api.entities.artifact;

import java.util.HashMap;
import java.util.Map;

import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class SessionLogsInput {

	/**
	 * Map of the service application app-ids and the corresponding hostnames. The map defines,
	 * which services are going to be tested.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("services")
	@JsonDeserialize(as = HashMap.class)
	private Map<AppId, String> services;

	/**
	 * The traces of the monitoring system, which are going to be used to generate the session logs.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("serializedTraces")
	private String serializedTraces;

	/**
	 * Constructor
	 *
	 * @param traces
	 *            the input trace
	 * @param services
	 *            the targeted services
	 */
	public SessionLogsInput(Map<AppId, String> services, String serializedTraces) {
		this.services = services;
		this.serializedTraces = serializedTraces;
	}

	/**
	 * Default Session Logs input for deserialization
	 */
	public SessionLogsInput() {

	}

	public Map<AppId, String> getServices() {
		return services;
	}

	public void setServices(Map<AppId, String> services) {
		this.services = services;
	}

	public String getSerializedTraces() {
		return serializedTraces;
	}

	public void setSerializedTraces(String serializedTraces) {
		this.serializedTraces = serializedTraces;
	}



}
