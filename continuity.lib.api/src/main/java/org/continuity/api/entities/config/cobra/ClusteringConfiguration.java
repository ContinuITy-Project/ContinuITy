package org.continuity.api.entities.config.cobra;

import java.time.Duration;

import org.continuity.api.entities.config.cobra.CobraConfiguration.DurationToStringConverter;
import org.continuity.api.entities.config.cobra.CobraConfiguration.StringToDurationConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusteringConfiguration {

	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration interval = Duration.ofHours(1);

	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration overlap = Duration.ofHours(5);

	@JsonProperty("avg-transition-tolerance")
	@JsonInclude(Include.NON_NULL)
	private Double avgTransitionTolerance = null;

	@JsonInclude(Include.NON_NULL)
	private Double epsilon = 1.5;

	@JsonIgnore
	private boolean epsilonSet = false;

	@JsonProperty("min-sample-size")
	private long minSampleSize = 10;

	private boolean omit = false;

	/**
	 * The interval at which the clustering should be triggered.
	 *
	 * @return
	 */
	public Duration getInterval() {
		return interval;
	}

	public void setInterval(Duration interval) {
		this.interval = interval;
	}

	/**
	 * The overlap between two subsequent clusterings, i.e., each clustering contains the latest
	 * {@code interval + overlap}.
	 *
	 * @return
	 */
	public Duration getOverlap() {
		return overlap;
	}

	public void setOverlap(Duration overlap) {
		this.overlap = overlap;
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
	public long getMinSampleSize() {
		return minSampleSize;
	}

	public void setMinSampleSize(long minSampleSize) {
		this.minSampleSize = minSampleSize;
	}

	/**
	 *
	 * @return Whether the automated clustering should be omitted. Defaults to {@code false}.
	 */
	public boolean isOmit() {
		return omit;
	}

	public void setOmit(boolean omitSessionClustering) {
		this.omit = omitSessionClustering;
	}

}
