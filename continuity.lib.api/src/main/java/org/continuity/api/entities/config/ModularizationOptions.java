package org.continuity.api.entities.config;

import java.util.Map;

import org.continuity.api.entities.deserialization.CustomMapDeserializer;
import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class ModularizationOptions {

	@JsonInclude(Include.NON_NULL)
	@JsonDeserialize(using = CustomMapDeserializer.class)
	private Map<AppId, String> services;

	@JsonProperty("modularization-approach")
	@JsonInclude(Include.NON_NULL)
	private ModularizationApproach modularizationApproach;

	public Map<AppId, String> getServices() {
		return services;
	}

	public void setServices(Map<AppId, String> services) {
		this.services = services;
	}

	public ModularizationApproach getModularizationApproach() {
		return modularizationApproach;
	}

	public void setModularizationApproach(ModularizationApproach modularizationApproach) {
		this.modularizationApproach = modularizationApproach;
	}

}
