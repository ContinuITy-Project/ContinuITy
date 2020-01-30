package org.continuity.dsl.timeseries;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

	@JsonIgnore
	private ContextRecord record;

	public NumericVariable() {
	}

	public NumericVariable(String name, double value, ContextRecord record) {
		this.name = name;
		this.value = value;
		this.record = record;
	}

	public String getName() {
		return name;
	}

	public double getValue() {
		return value;
	}

	/**
	 * Also modifies the entry in the record.
	 * @param value
	 */
	public void setValue(double value) {
		this.value = value;

		if (record != null) {
			record.getNumeric().put(name, value);
		}
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
