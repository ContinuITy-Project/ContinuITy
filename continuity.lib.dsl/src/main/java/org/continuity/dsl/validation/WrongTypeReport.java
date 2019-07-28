package org.continuity.dsl.validation;

import java.util.Objects;

import org.continuity.dsl.schema.VariableType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "name", "expected", "received" })
public class WrongTypeReport {

	private String name;

	private VariableType expected;

	private VariableType received;

	public WrongTypeReport() {
	}

	public WrongTypeReport(String name, VariableType expected, VariableType received) {
		this.name = name;
		this.expected = expected;
		this.received = received;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public VariableType getExpected() {
		return expected;
	}

	public void setExpected(VariableType expected) {
		this.expected = expected;
	}

	public VariableType getReceived() {
		return received;
	}

	public void setReceived(VariableType received) {
		this.received = received;
	}

	@Override
	public int hashCode() {
		return Objects.hash(expected, name, received);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof WrongTypeReport)) {
			return false;
		}
		WrongTypeReport other = (WrongTypeReport) obj;
		return (expected == other.expected) && Objects.equals(name, other.name) && (received == other.received);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(name).append(": ").append(received).append(" (expected ").append(expected).append(")").toString();
	}

}
