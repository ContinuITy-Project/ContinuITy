package org.continuity.forecast.context;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = BooleanCovariateValue.class)
public class BooleanCovariateValue extends GeneralCovariateValue implements CovariateValue {
	
	private boolean value;

	public boolean isValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "BooleanCovariateValue [value=" + value + "]";
	}

}
