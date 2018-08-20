package org.continuity.idpa.annotation;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataType {

	STRING,

	NUMBER,
	
	OBJECT,
	
	ARRAY;

	private static final Map<String, DataType> prettyStringToGoal = new HashMap<>();

	static {
		for (DataType type : values()) {
			prettyStringToGoal.put(type.toPrettyString(), type);
		}
	}

	@JsonCreator
	public static DataType fromPrettyString(String key) {
		return prettyStringToGoal.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().toLowerCase();
	}
}
