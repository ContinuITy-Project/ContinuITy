package org.continuity.api.entities.exchange;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Henning Schulz
 *
 */
public enum BehaviorModelType {

	MARKOV_CHAIN, REQUEST_RATES;

	private static final Map<String, BehaviorModelType> prettyStringToType = new HashMap<>();

	static {
		for (BehaviorModelType type : values()) {
			prettyStringToType.put(type.toPrettyString(), type);
		}
	}

	@JsonCreator
	public static BehaviorModelType fromPrettyString(String key) {
		return prettyStringToType.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

}
