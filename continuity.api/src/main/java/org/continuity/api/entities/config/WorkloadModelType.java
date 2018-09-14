package org.continuity.api.entities.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkloadModelType {
	WESSBAS, REQUEST_RATES;

	private static final Map<String, WorkloadModelType> prettyStringToType = new HashMap<>();

	static {
		for (WorkloadModelType type : values()) {
			prettyStringToType.put(type.toPrettyString(), type);
		}
	}

	@JsonCreator
	public static WorkloadModelType fromPrettyString(String key) {
		return prettyStringToType.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return name().replace("_", "-").toLowerCase();
	}
}
