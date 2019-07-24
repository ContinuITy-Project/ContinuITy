package org.continuity.api.entities.order;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TailoringApproach {
	LOG_BASED, MODEL_BASED;

	private static final Map<String, TailoringApproach> prettyStringToApproach = new HashMap<>();

	static {
		for (TailoringApproach approach : values()) {
			prettyStringToApproach.put(approach.toPrettyString(), approach);
		}
	}

	@JsonCreator
	public static TailoringApproach fromPrettyString(String key) {
		return prettyStringToApproach.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}
}
