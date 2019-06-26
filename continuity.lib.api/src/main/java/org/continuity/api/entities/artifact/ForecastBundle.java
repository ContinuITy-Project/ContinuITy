package org.continuity.api.entities.artifact;

import java.util.Date;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 
 * @author Alper Hidiroglu
 *
 */
public class ForecastBundle {
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH-mm-ss-SSSX")
	private Date timestamp;

	private int workloadIntensity;

	private LinkedList<Double> probabilities;

	public ForecastBundle(Date timestamp, Integer workloadIntensity, LinkedList<Double> probabilities) {
		this.timestamp = timestamp;
		this.workloadIntensity = workloadIntensity;
		this.probabilities = probabilities;
	}

	public ForecastBundle() {
		
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
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
