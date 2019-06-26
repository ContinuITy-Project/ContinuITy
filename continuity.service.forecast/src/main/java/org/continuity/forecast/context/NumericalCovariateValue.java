package org.continuity.forecast.context;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = NumericalCovariateValue.class)
public class NumericalCovariateValue extends GeneralCovariateValue implements CovariateValue {
	
	private double value;

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "NumericalCovariateValue [value=" + value + "]";
	}

}
