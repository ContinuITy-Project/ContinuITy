package org.continuity.dsl.validation;

import java.util.Objects;

import org.continuity.dsl.schema.VariableType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "name", "type" })
public class NewVariableReport {

	private String name;

	private VariableType type;

	public NewVariableReport() {
	}

	public NewVariableReport(String name, VariableType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public VariableType getType() {
		return type;
	}

	public void setType(VariableType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NewVariableReport)) {
			return false;
		}
		NewVariableReport other = (NewVariableReport) obj;
		return Objects.equals(name, other.name) && (type == other.type);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(name).append(": ").append(type).toString();
	}

}
