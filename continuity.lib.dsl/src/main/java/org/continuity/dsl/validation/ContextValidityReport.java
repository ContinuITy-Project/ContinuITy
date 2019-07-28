package org.continuity.dsl.validation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.dsl.schema.VariableType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Henning Schulz
 *
 */
public class ContextValidityReport {

	private List<NewVariableReport> unknown;

	@JsonProperty("newly-added")
	private List<NewVariableReport> newlyAdded;

	private List<WrongTypeReport> wrongType;

	public boolean isErroenous() {
		return ((unknown != null) && !unknown.isEmpty()) || ((wrongType != null) && !wrongType.isEmpty());
	}

	@JsonIgnore
	public boolean hasNew() {
		return (newlyAdded != null) && !newlyAdded.isEmpty();
	}

	public List<NewVariableReport> getNewVariables() {
		return unknown;
	}

	public void setNewVariables(List<NewVariableReport> newVariables) {
		this.unknown = newVariables;
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

	public void reportUnknown(String name, VariableType type) {
		if (unknown == null) {
			synchronized (this) {
				if (unknown == null) {
					unknown = new ArrayList<>();
				}
			}
		}

		unknown.add(new NewVariableReport(name, type));
	}

	public void reportNewlyAdded(String name, VariableType type) {
		if (newlyAdded == null) {
			synchronized (this) {
				if (newlyAdded == null) {
					newlyAdded = new ArrayList<>();
				}
			}
		}

		newlyAdded.add(new NewVariableReport(name, type));
	}

	public void reportWrongType(String name, VariableType expected, VariableType received) {
		if (wrongType == null) {
			synchronized (this) {
				if (wrongType == null) {
					wrongType = new ArrayList<>();
				}
			}
		}

		wrongType.add(new WrongTypeReport(name, expected, received));
	}

	public ContextValidityReport merge(ContextValidityReport other) {
		if (other != null) {
			this.unknown.addAll(other.unknown);
			this.newlyAdded.addAll(other.newlyAdded);
			this.wrongType.addAll(other.wrongType);
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
