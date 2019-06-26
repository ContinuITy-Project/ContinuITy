package org.continuity.api.entities.config;

import java.util.Map;

import org.continuity.api.entities.deserialization.CustomMapDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class ModularizationOptions {

	@JsonInclude(Include.NON_NULL)
	@JsonDeserialize(using = CustomMapDeserializer.class)
	private Map<String, String> services;

	@JsonProperty("modularization-approach")
	@JsonInclude(Include.NON_NULL)
	private ModularizationApproach modularizationApproach;

	public Map<String, String> getServices() {
		return services;
	}

	public void setServices(Map<String, String> services) {
		this.services = services;
	}

	public ModularizationApproach getModularizationApproach() {
		return modularizationApproach;
	}

	public void setModularizationApproach(ModularizationApproach modularizationApproach) {
		this.modularizationApproach = modularizationApproach;
	}

}
