package org.continuity.api.entities.artifact;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class SessionLogsInput {

	/**
	 * Map of the service application tags and the corresponding hostnames. The map defines, which
	 * services are going to be tested.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("services")
	@JsonDeserialize(as = HashMap.class)
	private Map<String, String> services;

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
	public SessionLogsInput(Map<String, String> services, String serializedTraces) {
		this.services = services;
		this.serializedTraces = serializedTraces;
	}
	
	/**
	 * Default Session Logs input for deserialization
	 */
	public SessionLogsInput() {
		
	}
	
	public Map<String, String> getServices() {
		return services;
	}

	public void setServices(Map<String, String> services) {
		this.services = services;
	}

	public String getSerializedTraces() {
		return serializedTraces;
	}

	public void setSerializedTraces(String serializedTraces) {
		this.serializedTraces = serializedTraces;
	}
	
	

}
