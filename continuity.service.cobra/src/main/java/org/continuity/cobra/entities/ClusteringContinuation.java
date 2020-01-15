package org.continuity.cobra.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Henning Schulz
 *
 */
public enum ClusteringContinuation {

	NO, IGNORING_TIMEOUT, RESPECTING_TIMEOUT;

	public static ClusteringContinuation fromBool(boolean ignoreTimeout, boolean continueWithNext) {
		if (continueWithNext && ignoreTimeout) {
			return IGNORING_TIMEOUT;
		} else if (continueWithNext && !ignoreTimeout) {
			return RESPECTING_TIMEOUT;
		} else {
			return NO;
		}
	}

	@JsonCreator
	public static ClusteringContinuation fromPrettyString(String key) {
		return key == null ? null : valueOf(key.toUpperCase().replace("-", "_"));
	}

	@JsonValue
	public String toPrettyString() {
		return toString().toLowerCase().replace("_", "-");
	}

}
