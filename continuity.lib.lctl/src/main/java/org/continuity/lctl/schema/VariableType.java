package org.continuity.lctl.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Summarizes types of context variables.
 *
 * @author Henning Schulz
 *
 */
public enum VariableType {

	NUMERIC, STRING, BOOLEAN;

	private static final Map<String, VariableType> prettyStringToType = new HashMap<>();

	static {
		for (VariableType type : values()) {
			prettyStringToType.put(type.toPrettyString(), type);
		}
	}

	@JsonCreator
	public static VariableType fromPrettyString(String key) {
		return prettyStringToType.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

}
