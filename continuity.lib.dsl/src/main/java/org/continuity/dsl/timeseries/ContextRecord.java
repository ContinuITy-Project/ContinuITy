package org.continuity.dsl.timeseries;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the context at one timestamp.
 *
 * @author Henning Schulz
 *
 */
public class ContextRecord {

	@JsonInclude(Include.NON_NULL)
	private Set<NumericVariable> numeric;

	@JsonInclude(Include.NON_NULL)
	private Set<StringVariable> string;

	@JsonProperty("boolean")
	@JsonInclude(Include.NON_NULL)
	private Set<String> bool;

	public Set<NumericVariable> getNumeric() {
		return numeric;
	}

	public void setNumeric(Set<NumericVariable> numeric) {
		this.numeric = numeric;
	}

	public Set<StringVariable> getString() {
		return string;
	}

	public void setString(Set<StringVariable> string) {
		this.string = string;
	}

	public Set<String> getBoolean() {
		return bool;
	}

	public void setBoolean(Set<String> bool) {
		this.bool = bool;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return isNullOrEmpty(numeric) && isNullOrEmpty(string) && isNullOrEmpty(bool);
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
				this.numeric = new HashSet<>();
			}

			this.numeric.addAll(other.numeric);
		}

		if (!isNullOrEmpty(other.string)) {
			if (this.string == null) {
				this.string = new HashSet<>();
			}

			this.string.addAll(other.string);
		}

		if (!isNullOrEmpty(other.bool)) {
			if (this.bool == null) {
				this.bool = new HashSet<>();
			}

			this.bool.addAll(other.bool);
		}
	}

	private boolean isNullOrEmpty(Collection<?> list) {
		return (list == null) || list.isEmpty();
	}

}
