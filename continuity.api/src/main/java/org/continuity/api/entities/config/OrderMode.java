package org.continuity.api.entities.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderMode {

	PAST_SESSIONS, PAST_REQUESTS, FORECASTED_WORKLOAD;

	private static final Map<String, OrderMode> prettyStringToMode = new HashMap<>();

	static {
		for (OrderMode mode : values()) {
			prettyStringToMode.put(mode.toPrettyString(), mode);
		}
	}

	@JsonCreator
	public static OrderMode fromPrettyString(String key) {
		return prettyStringToMode.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return name().replace("_", "-").toLowerCase();
	}

}
