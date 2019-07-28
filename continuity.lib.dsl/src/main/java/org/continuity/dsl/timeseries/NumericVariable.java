package org.continuity.dsl.timeseries;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Defines a single numeric variable.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "name", "value" })
public class NumericVariable {

	private String name;

	private double value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

}
