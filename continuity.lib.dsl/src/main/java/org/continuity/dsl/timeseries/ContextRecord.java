package org.continuity.dsl.timeseries;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
	private Map<String, Double> numeric;

	@JsonInclude(Include.NON_NULL)
	private Map<String, String> string;

	@JsonProperty("boolean")
	@JsonInclude(Include.NON_NULL)
	private Set<String> bool;

	public Map<String, Double> getNumeric() {
		return numeric;
	}

	@JsonIgnore
	public Set<NumericVariable> getNumericVariables() {
		if (numeric == null) {
			return Collections.emptySet();
		}

		return numeric.entrySet().stream().map(e -> new NumericVariable(e.getKey(), e.getValue(), this)).collect(Collectors.toSet());
	}

	public void setNumeric(Map<String, Double> numeric) {
		this.numeric = numeric;
	}

	public Map<String, String> getString() {
		return string;
	}

	@JsonIgnore
	public Set<StringVariable> getStringVariables() {
		if (string == null) {
			return Collections.emptySet();
		}

		return string.entrySet().stream().map(e -> new StringVariable(e.getKey(), e.getValue(), this)).collect(Collectors.toSet());
	}

	public void setString(Map<String, String> string) {
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
				this.numeric = new HashMap<>();
			}

			this.numeric.putAll(other.numeric);
		}

		if (!isNullOrEmpty(other.string)) {
			if (this.string == null) {
				this.string = new HashMap<>();
			}

			this.string.putAll(other.string);
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

	private boolean isNullOrEmpty(Map<?, ?> map) {
		return (map == null) || map.isEmpty();
	}

}
