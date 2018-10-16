package org.continuity.api.entities.artifact;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The modularized session logs.
 * @author Tobias Angerstein
 *
 */
public class ModularizedSessionLogs extends SessionLogs {
	/**
	 * The normal distributions of the pre and post processing time for each modularized request.
	 */
	@JsonSerialize(as=HashMap.class)
	private Map<String, ProcessingTimeNormalDistributions> normalDistributions;

	public ModularizedSessionLogs(Map<String, ProcessingTimeNormalDistributions> normalDistributions) {
		super();
		this.normalDistributions = normalDistributions;
	}
	/**
	 * Default constructor.
	 */
	public ModularizedSessionLogs() {
	}
	
	public Map<String, ProcessingTimeNormalDistributions> getNormalDistributions() {
		return normalDistributions;
	}

	public void setPreProcessingMeanTime(Map<String, ProcessingTimeNormalDistributions> normalDistributions) {
		this.normalDistributions = normalDistributions;
	}
	
}


	
	

