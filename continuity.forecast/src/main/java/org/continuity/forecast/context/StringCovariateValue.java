package org.continuity.forecast.context;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = StringCovariateValue.class)
public class StringCovariateValue extends GeneralCovariateValue implements CovariateValue {
	
	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "StringCovariateValue [value=" + value + "]";
	}

}
