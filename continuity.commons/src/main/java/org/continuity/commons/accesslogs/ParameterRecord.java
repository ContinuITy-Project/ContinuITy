package org.continuity.commons.accesslogs;

public class ParameterRecord {

	private String name;

	private String value;

	public ParameterRecord(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public static ParameterRecord fromString(String param) {
		String[] nameAndValue = param.split("=");
		return new ParameterRecord(nameAndValue[0], nameAndValue.length > 1 ? nameAndValue[1] : null);
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
