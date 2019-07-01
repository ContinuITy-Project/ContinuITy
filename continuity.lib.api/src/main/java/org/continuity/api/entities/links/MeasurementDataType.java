package org.continuity.api.entities.links;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MeasurementDataType {
	OPEN_XTRACE, INSPECTIT, CSV, ACCESS_LOGS;

	private static final Map<String, MeasurementDataType> prettyStringToApproach = new HashMap<>();

	static {
		for (MeasurementDataType approach : values()) {
			prettyStringToApproach.put(approach.toPrettyString(), approach);
		}
	}

	@JsonCreator
	public static MeasurementDataType fromPrettyString(String key) {
		return prettyStringToApproach.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

	public static class Converter implements org.springframework.core.convert.converter.Converter<String, MeasurementDataType> {

		@Override
		public MeasurementDataType convert(String source) {
			return fromPrettyString(source);
		}

	}

}
