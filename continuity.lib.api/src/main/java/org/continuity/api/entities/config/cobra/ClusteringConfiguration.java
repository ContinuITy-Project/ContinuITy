package org.continuity.api.entities.config.cobra;

import java.time.Duration;

import org.continuity.api.entities.config.cobra.CobraConfiguration.DurationToStringConverter;
import org.continuity.api.entities.config.cobra.CobraConfiguration.StringToDurationConverter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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

	private long lookback = 10;

	@JsonInclude(Include.NON_NULL)
	private Long dimensions;

	private AppendStrategyConfiguration initial = AppendStrategyConfiguration.defaultKmeans();

	private AppendStrategyConfiguration append = AppendStrategyConfiguration.defaultMinimumDistance();

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
	 * The number of previous clustering to look back in the past when correlating the new one.
	 *
	 * @return
	 */
	public long getLookback() {
		return lookback;
	}

	public void setLookback(long lookback) {
		this.lookback = lookback;
	}

	/**
	 * The number of dimensions to which the sessions should be reduced for clustering.
	 *
	 * @return
	 */
	public Long getDimensions() {
		return dimensions;
	}

	public void setDimensions(long dimension) {
		this.dimensions = dimension;
	}

	/**
	 * Configuration of initial clustering.
	 *
	 * @return
	 */
	public AppendStrategyConfiguration getInitial() {
		return initial;
	}

	public void setInitial(AppendStrategyConfiguration initial) {
		this.initial = initial;
	}

	/**
	 * Configuration of the strategy defining how to append new sessions.
	 *
	 * @return
	 */
	public AppendStrategyConfiguration getAppend() {
		return append;
	}

	public void setAppend(AppendStrategyConfiguration append) {
		this.append = append;
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
