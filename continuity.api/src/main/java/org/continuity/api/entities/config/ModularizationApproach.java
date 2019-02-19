package org.continuity.api.entities.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ModularizationApproach {
	SESSION_LOGS, WORKLOAD_MODEL, REQUESTS;

	private static final Map<String, ModularizationApproach> prettyStringToApproach = new HashMap<>();

	static {
		for (ModularizationApproach approach : values()) {
			prettyStringToApproach.put(approach.toPrettyString(), approach);
		}
	}

	@JsonCreator
	public static ModularizationApproach fromPrettyString(String key) {
		return prettyStringToApproach.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}
}
