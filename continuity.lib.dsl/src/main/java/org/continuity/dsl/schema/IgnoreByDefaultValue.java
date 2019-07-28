package org.continuity.dsl.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Henning Schulz
 *
 */
public enum IgnoreByDefaultValue {

	TRUE, FALSE, ONLY_NEW;

	private static final Map<String, IgnoreByDefaultValue> prettyStringToValue = new HashMap<>();

	static {
		for (IgnoreByDefaultValue type : values()) {
			prettyStringToValue.put(type.toPrettyString(), type);
		}
	}

	public boolean ignoreNew() {
		return this != FALSE;
	}

	/**
	 *
	 * @param ignoreInner
	 * @return Whether a given variable should be ignored. The inner specification always overwrites
	 *         the outer if present.
	 */
	public boolean ignore(Optional<Boolean> ignoreInner) {
		return ignoreInner.orElse(this == TRUE);
	}

	@JsonCreator
	public static IgnoreByDefaultValue fromPrettyString(String key) {
		return prettyStringToValue.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

}
