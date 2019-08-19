package org.continuity.dsl.timeseries;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Defines a single string variable.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "name", "value" })
public class StringVariable {

	private String name;

	private String value;

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
	public boolean equals(Object obj) {
		if ((obj == null) || !(obj instanceof StringVariable)) {
			return false;
		}

		StringVariable other = (StringVariable) obj;

		return Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

}
