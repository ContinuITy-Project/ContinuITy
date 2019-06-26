package org.continuity.api.entities.artifact;

import java.util.List;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Holds the pre and post processing time distribution of a certain modularized request.
 * 
 * @author Tobias Angerstein
 *
 */
public class ProcessingTimeNormalDistributions {
	/**
	 * The mean of the preprocessing time in millis.
	 */
	private double preprocessingTimeMean;

	/**
	 * The deviation of the preprocessing time deviation in millis.
	 */
	private double preprocessingTimeDeviation;

	/**
	 * The mean of the preprocessing time.
	 */
	private double postprocessingTimeMean;

	/**
	 * The deviation of the preprocessing time deviation.
	 */
	private double postprocessingTimeDeviation;

	/**
	 * Constructor.
	 * 
	 * @param preprocessingTimeMeasures
	 *            all pre processing time measures
	 * @param postprocessingTimeMeasures
	 *            all post processing time measures
	 */
	public ProcessingTimeNormalDistributions(List<Double> preprocessingTimeMeasures, List<Double> postprocessingTimeMeasures) {
		// Calculate normal distribution of the pre processing time
		preprocessingTimeMean = StatUtils.mean(preprocessingTimeMeasures.stream().mapToDouble(i -> i).toArray());
		preprocessingTimeDeviation = Math.sqrt(StatUtils.variance(preprocessingTimeMeasures.stream().mapToDouble(i -> i).toArray()));

		// Calculate normal distribution of the post processing time
		postprocessingTimeMean = StatUtils.mean(postprocessingTimeMeasures.stream().mapToDouble(i -> i).toArray());
		postprocessingTimeDeviation = Math.sqrt(StatUtils.variance(postprocessingTimeMeasures.stream().mapToDouble(i -> i).toArray()));
	}
	
	/**
	 * Default time.
	 */
	public ProcessingTimeNormalDistributions() {
	}

	// GETTERS
	/**
	 * Returns the preprocessing time mean in millis.
	 * @return
	 */
	public double getPreprocessingTimeMean() {
		return preprocessingTimeMean;
	}
	
	/**
	 * Returns the preprocessing time deviation in millis.
	 * @return
	 */
	public double getPreprocessingTimeDeviation() {
		return preprocessingTimeDeviation;
	}
	
	/**
	 * Returns the postprocessing time mean in millis.
	 * @return
	 */
	public double getPostprocessingTimeMean() {
		return postprocessingTimeMean;
	}
	
	
	/**
	 * Returns the postprocessing time deviation in millis.
	 * @return 
	 */
	public double getPostprocessingTimeDeviation() {
		return postprocessingTimeDeviation;
	}

}