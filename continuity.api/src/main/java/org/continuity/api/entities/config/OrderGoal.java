package org.continuity.api.entities.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderGoal {

	CREATE_SESSION_LOGS, CREATE_BEHAVIOR_MIX, CREATE_FORECAST, CREATE_WORKLOAD_MODEL, CREATE_LOAD_TEST, EXECUTE_LOAD_TEST;

	private static final Map<String, OrderGoal> prettyStringToGoal = new HashMap<>();

	static {
		for (OrderGoal goal : values()) {
			prettyStringToGoal.put(goal.toPrettyString(), goal);
		}
	}

	@JsonCreator
	public static OrderGoal fromPrettyString(String key) {
		return prettyStringToGoal.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return name().replace("_", "-").toLowerCase();
	}

}
