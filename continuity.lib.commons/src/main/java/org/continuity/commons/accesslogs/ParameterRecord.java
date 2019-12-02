package org.continuity.commons.accesslogs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterRecord {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParameterRecord.class);

	private String name;

	private String value;

	public ParameterRecord(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public static ParameterRecord fromString(String param) {
		int equalsIndex = param.indexOf("=");

		String name;
		String value;

		if (equalsIndex < 0) {
			name = param;
			value = null;
		} else {
			name = param.substring(0, equalsIndex);
			value = param.substring(equalsIndex + 1);
		}

		if (name.length() == 0) {
			LOGGER.warn("Ignoring empty parameter: '{}'", param);
			return null;
		}

		return new ParameterRecord(name, value);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		if (value == null) {
			return name;
		} else {
			return new StringBuilder().append(name).append("=").append(value).toString();
		}
	}

}
