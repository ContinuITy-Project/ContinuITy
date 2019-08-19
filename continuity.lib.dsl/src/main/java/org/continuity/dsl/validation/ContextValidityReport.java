package org.continuity.dsl.validation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.dsl.schema.VariableType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "erroenous", "unknown", "newly-added", "wrong-type" })
public class ContextValidityReport {

	@JsonInclude(Include.NON_EMPTY)
	private List<NewVariableReport> unknown;

	@JsonProperty("newly-added")
	@JsonInclude(Include.NON_EMPTY)
	private List<NewVariableReport> newlyAdded;

	@JsonProperty("wrong-type")
	@JsonInclude(Include.NON_EMPTY)
	private List<WrongTypeReport> wrongType;

	public boolean isErroenous() {
		return ((unknown != null) && !unknown.isEmpty()) || ((wrongType != null) && !wrongType.isEmpty());
	}

	@JsonIgnore
	public boolean hasNew() {
		return (newlyAdded != null) && !newlyAdded.isEmpty();
	}

	public List<NewVariableReport> getUnknown() {
		return unknown;
	}

	public void setUnknown(List<NewVariableReport> unknown) {
		this.unknown = unknown;
	}

	public List<NewVariableReport> getNewlyAdded() {
		return newlyAdded;
	}

	public void setNewlyAdded(List<NewVariableReport> newlyAdded) {
		this.newlyAdded = newlyAdded;
	}

	public List<WrongTypeReport> getWrongType() {
		return wrongType;
	}

	public void setWrongType(List<WrongTypeReport> wrongType) {
		this.wrongType = wrongType;
	}

	@JsonIgnore
	private List<NewVariableReport> getUnknownNonNull() {
		if (unknown == null) {
			synchronized (this) {
				if (unknown == null) {
					unknown = new ArrayList<>();
				}
			}
		}

		return unknown;
	}

	@JsonIgnore
	private List<NewVariableReport> getNewlyAddedNonNull() {
		if (newlyAdded == null) {
			synchronized (this) {
				if (newlyAdded == null) {
					newlyAdded = new ArrayList<>();
				}
			}
		}

		return newlyAdded;
	}

	@JsonIgnore
	private List<WrongTypeReport> getWrongTypeNonNull() {
		if (wrongType == null) {
			synchronized (this) {
				if (wrongType == null) {
					wrongType = new ArrayList<>();
				}
			}
		}

		return wrongType;
	}

	public void reportUnknown(String name, VariableType type) {
		getUnknownNonNull().add(new NewVariableReport(name, type));
	}

	public void reportNewlyAdded(String name, VariableType type) {
		getNewlyAddedNonNull().add(new NewVariableReport(name, type));
	}

	public void reportWrongType(String name, VariableType expected, VariableType received) {
		getWrongTypeNonNull().add(new WrongTypeReport(name, expected, received));
	}

	public ContextValidityReport merge(ContextValidityReport other) {
		if (other != null) {
			if (other.getUnknown() != null) {
				this.getUnknownNonNull().addAll(other.getUnknown());
			}

			if (other.getNewlyAdded() != null) {
				this.getNewlyAddedNonNull().addAll(other.getNewlyAdded());
			}

			if (other.getWrongType() != null) {
				this.getWrongTypeNonNull().addAll(other.getWrongType());
			}
		}

		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("unknown: ").append(unknown).append("\n");
		builder.append("newly-added: ").append(newlyAdded).append("\n");
		builder.append("wrong-type: ").append(wrongType);

		return builder.toString();
	}

}
