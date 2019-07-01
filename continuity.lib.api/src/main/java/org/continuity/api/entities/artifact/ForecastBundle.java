package org.continuity.api.entities.artifact;

import java.util.LinkedList;

import org.continuity.idpa.VersionOrTimestamp;

/**
 *
 * @author Alper Hidiroglu
 *
 */
public class ForecastBundle {

	private VersionOrTimestamp version;

	private int workloadIntensity;

	private LinkedList<Double> probabilities;

	public ForecastBundle(VersionOrTimestamp version, Integer workloadIntensity, LinkedList<Double> probabilities) {
		this.version = version;
		this.workloadIntensity = workloadIntensity;
		this.probabilities = probabilities;
	}

	public ForecastBundle() {

	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public Integer getWorkloadIntensity() {
		return workloadIntensity;
	}

	public void setWorkloadIntensity(Integer workloadIntensity) {
		this.workloadIntensity = workloadIntensity;
	}

	public LinkedList<Double> getProbabilities() {
		return probabilities;
	}

	public void setProbabilities(LinkedList<Double> probabilities) {
		this.probabilities = probabilities;
	}

}
