package org.continuity.api.entities.config.cobra;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Strategy how to append new sessions to the existing behavior model.
 *
 * @author Henning Schulz
 *
 */
public enum AppendStrategy {

	DBSCAN(false, true), KMEANS(false, true), MINIMUM_DISTANCE(false, false) {
		@Override
		public long getLookback(long configuredLookback) {
			return configuredLookback > 0 ? 1 : 0;
		}
	};

	private final boolean overwriteExisting;

	private final boolean includeOverlap;

	private AppendStrategy(boolean overwriteExisting, boolean includeOverlap) {
		this.overwriteExisting = overwriteExisting;
		this.includeOverlap = includeOverlap;
	}

	@JsonIgnore
	public boolean overwriteExisting() {
		return overwriteExisting;
	}

	@JsonIgnore
	public boolean includeOverlap() {
		return includeOverlap;
	}

	@JsonIgnore
	public long getLookback(long configuredLookback) {
		return configuredLookback;
	}

	@JsonCreator
	public static AppendStrategy fromPrettyString(String key) {
		return key == null ? null : valueOf(key.toUpperCase().replace("-", "_"));
	}

	@JsonValue
	public String toPrettyString() {
		return toString().toLowerCase().replace("_", "-");
	}

}
