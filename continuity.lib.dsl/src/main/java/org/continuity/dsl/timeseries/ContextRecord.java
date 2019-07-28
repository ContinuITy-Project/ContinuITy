package org.continuity.dsl.timeseries;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the context at one timestamp.
 *
 * @author Henning Schulz
 *
 */
public class ContextRecord {

	private List<NumericVariable> numeric;

	private List<StringVariable> string;

	@JsonProperty("boolean")
	private List<String> bool;

	public List<NumericVariable> getNumeric() {
		return numeric;
	}

	public void setNumeric(List<NumericVariable> numeric) {
		this.numeric = numeric;
	}

	public List<StringVariable> getString() {
		return string;
	}

	public void setString(List<StringVariable> string) {
		this.string = string;
	}

	public List<String> getBoolean() {
		return bool;
	}

	public void setBoolean(List<String> bool) {
		this.bool = bool;
	}

	/**
	 * Adds all context variables from another record to this.
	 *
	 * @param other
	 *            The other record.
	 */
	@JsonIgnore
	public void merge(ContextRecord other) {
		if (other == null) {
			return;
		}

		if (!isNullOrEmpty(other.numeric)) {
			if (this.numeric == null) {
				this.numeric = new ArrayList<>();
			}

			this.numeric.addAll(other.numeric);
		}

		if (!isNullOrEmpty(other.string)) {
			if (this.string == null) {
				this.string = new ArrayList<>();
			}

			this.string.addAll(other.string);
		}

		if (!isNullOrEmpty(other.bool)) {
			if (this.bool == null) {
				this.bool = new ArrayList<>();
			}

			this.bool.addAll(other.bool);
		}
	}

	private boolean isNullOrEmpty(List<?> list) {
		return (list == null) || list.isEmpty();
	}

}
