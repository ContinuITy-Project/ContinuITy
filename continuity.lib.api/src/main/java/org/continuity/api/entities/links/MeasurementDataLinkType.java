package org.continuity.api.entities.links;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MeasurementDataLinkType {
	OPEN_XTRACE, INSPECTIT, CSV;

	private static final Map<String, MeasurementDataLinkType> prettyStringToApproach = new HashMap<>();

	static {
		for (MeasurementDataLinkType approach : values()) {
			prettyStringToApproach.put(approach.toPrettyString(), approach);
		}
	}

	@JsonCreator
	public static MeasurementDataLinkType fromPrettyString(String key) {
		return prettyStringToApproach.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}
}
