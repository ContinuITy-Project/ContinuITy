package org.continuity.api.entities.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkloadModelType {
	WESSBAS;

	@JsonCreator
	public static OrderGoal fromPrettyString(String key) {
		return key == null ? null : OrderGoal.valueOf(key.toUpperCase());
	}

	@JsonValue
	public String toPrettyString() {
		return toString().toLowerCase();
	}
}
