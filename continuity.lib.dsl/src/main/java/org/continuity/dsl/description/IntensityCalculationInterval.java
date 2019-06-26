package org.continuity.dsl.description;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IntensityCalculationInterval {
	SECOND {

		@Override
		public long asNumber() {
			return 1000000000L;
		}

	},
	MINUTE {

		@Override
		public long asNumber() {
			return 60000000000L;
		}

	},
	HOUR {

		@Override
		public long asNumber() {
			return 3600000000000L;
		}

	},
	DAY {

		@Override
		public long asNumber() {
			return 86400000000000L;
		}

	};

	private static final Map<String, IntensityCalculationInterval> prettyStringToInterval = new HashMap<>();

	static {
		for (IntensityCalculationInterval interval : values()) {
			prettyStringToInterval.put(interval.toPrettyString(), interval);
		}
	}

	@JsonCreator
	public static IntensityCalculationInterval fromPrettyString(String key) {
		 IntensityCalculationInterval interval = prettyStringToInterval.get(key);
		 if (null == interval) {
			 return IntensityCalculationInterval.SECOND;
		 } else {
			 return interval;
		 }
	}

	@JsonValue
	public String toPrettyString() {
		return name().replace("_", "-").toLowerCase();
	}
	/**
	 * Returns interval in nanos
	 * @return interval in nanos
	 */
	public abstract long asNumber();
}
