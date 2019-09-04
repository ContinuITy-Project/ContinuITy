package org.continuity.dsl.timeseries;

import java.util.Objects;

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

	public NumericVariable() {
	}

	public NumericVariable(String name, double value) {
		this.name = name;
		this.value = value;
	}

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

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || !(obj instanceof NumericVariable)) {
			return false;
		}

		NumericVariable other = (NumericVariable) obj;

		return Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

}
