package org.continuity.api.entities.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LoadTestType {

	JMETER(true), BENCHFLOW(false);

	private final boolean canExecute;

	private LoadTestType(boolean canExecute) {
		this.canExecute = canExecute;
	}

	public boolean canExecute() {
		return canExecute;
	}

	@JsonCreator
	public static OrderGoal fromPrettyString(String key) {
		return key == null ? null : OrderGoal.valueOf(key.toUpperCase());
	}

	@JsonValue
	public String toPrettyString() {
		return toString().toLowerCase();
	}

}
