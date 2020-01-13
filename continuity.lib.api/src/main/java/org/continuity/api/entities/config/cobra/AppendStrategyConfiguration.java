package org.continuity.api.entities.config.cobra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppendStrategyConfiguration {

	private AppendStrategy strategy = AppendStrategy.KMEANS;

	@JsonProperty("avg-transition-tolerance")
	@JsonInclude(Include.NON_NULL)
	private Double avgTransitionTolerance = null;

	@JsonInclude(Include.NON_NULL)
	private Double epsilon = null;

	@JsonIgnore
	private boolean epsilonSet = false;

	@JsonProperty("min-sample-size")
	@JsonInclude(Include.NON_NULL)
	private Long minSampleSize = null;

	@JsonInclude(Include.NON_NULL)
	private Long k = null;

	@JsonInclude(Include.NON_NULL)
	private Integer parallelize = null;

	private AppendStrategyConfiguration(AppendStrategy strategy, Double avgTransitionTolerance, Double epsilon, boolean epsilonSet, Long minSampleSize, Long k, Integer parallelize) {
		this.strategy = strategy;
		this.avgTransitionTolerance = avgTransitionTolerance;
		this.epsilon = epsilon;
		this.epsilonSet = epsilonSet;
		this.minSampleSize = minSampleSize;
		this.k = k;
		this.parallelize = parallelize;
	}

	public AppendStrategyConfiguration() {
	}

	public static AppendStrategyConfiguration defaultDbscan() {
		return new AppendStrategyConfiguration(AppendStrategy.DBSCAN, null, 1.5, true, 10L, null, null);
	}

	public static AppendStrategyConfiguration defaultKmeans() {
		return new AppendStrategyConfiguration(AppendStrategy.KMEANS, null, null, false, null, 5L, 1);
	}

	public static AppendStrategyConfiguration defaultMinimumDistance() {
		return new AppendStrategyConfiguration(AppendStrategy.MINIMUM_DISTANCE, null, null, false, null, null, null);
	}

	/**
	 * The strategy defining how to cluster/append new sessions.
	 *
	 * @return
	 */
	public AppendStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(AppendStrategy strategy) {
		this.strategy = strategy;
	}

	/**
	 * The average transition tolerance for the DBSCAN algorithm. Corresponds to the epsilon
	 * parameter with {@code n} = number of endpoints:
	 * {@code epsilon = (n + 1) * avgTransitionTolerance}.
	 *
	 * @return
	 */
	public Double getAvgTransitionTolerance() {
		return avgTransitionTolerance;
	}

	public void setAvgTransitionTolerance(double avgTransitionTolerance) {
		this.avgTransitionTolerance = avgTransitionTolerance;

		if (!this.epsilonSet) {
			this.epsilon = null;
		}
	}

	/**
	 * The epsilon parameter for the DBSCAN algorithm. Alternative to
	 * {@link #getAvgTransitionTolerance()}.
	 *
	 * @return
	 */
	public Double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
		this.epsilonSet = true;
	}

	/**
	 * The min sample size parameter for the DBSCAN algorithm.
	 *
	 * @return
	 */
	public Long getMinSampleSize() {
		return minSampleSize;
	}

	public void setMinSampleSize(long minSampleSize) {
		this.minSampleSize = minSampleSize;
	}

	/**
	 * The parameter of the KMeans algorithm.
	 *
	 * @return
	 */
	public Long getK() {
		return k;
	}

	public void setK(long k) {
		this.k = k;
	}

	/**
	 * The number of jobs to run in parallel. Negative numbers mean
	 * {@code num_cpus + 1 + parallelize}, i.e., -1 means all CPUs, -2 all except for one, etc.
	 * Currently, only supported by kmeans.
	 *
	 * @return
	 */
	public Integer getParallelize() {
		return parallelize;
	}

	public void setParallelize(int parallelize) {
		this.parallelize = parallelize;
	}

}
