package org.continuity.api.entities.links;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SessionsStatus {
	
	CHANGED, NOT_CHANGED;

	private static final Map<String, SessionsStatus> prettyStringToApproach = new HashMap<>();

	static {
		for (SessionsStatus status : values()) {
			prettyStringToApproach.put(status.toPrettyString(), status);
		}
	}

	@JsonCreator
	public static SessionsStatus fromPrettyString(String key) {
		return prettyStringToApproach.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}
}
