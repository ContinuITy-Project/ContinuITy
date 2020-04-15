package org.continuity.lctl.timeseries;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

	@JsonIgnore
	private ContextRecord record;

	public StringVariable() {
	}

	public StringVariable(String name, String value, ContextRecord record) {
		this.name = name;
		this.value = value;
		this.record = record;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Also modifies the entry in the record.
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;

		if (record != null) {
			record.getString().put(name, value);
		}
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
