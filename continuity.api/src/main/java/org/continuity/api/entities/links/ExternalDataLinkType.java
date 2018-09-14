package org.continuity.api.entities.links;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExternalDataLinkType {
	OPEN_XTRACE, INSPECTIT, CSV;

	private static final Map<String, ExternalDataLinkType> prettyStringToApproach = new HashMap<>();

	static {
		for (ExternalDataLinkType approach : values()) {
			prettyStringToApproach.put(approach.toPrettyString(), approach);
		}
	}

	@JsonCreator
	public static ExternalDataLinkType fromPrettyString(String key) {
		return prettyStringToApproach.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}
}
