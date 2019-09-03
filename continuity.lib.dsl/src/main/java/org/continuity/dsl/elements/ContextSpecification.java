package org.continuity.dsl.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.continuity.dsl.ContextValue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Specifies a context
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "is", "multiplied", "added", "during" })
public class ContextSpecification {

	@JsonInclude(Include.NON_ABSENT)
	private Optional<ContextValue> is = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Double> multiplied = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Double> added = Optional.empty();

	@JsonInclude(Include.NON_EMPTY)
	private List<TimeSpecification> during = new ArrayList<>();

	public Optional<ContextValue> getIs() {
		return is;
	}

	public ContextSpecification setIs(ContextValue is) {
		this.is = Optional.ofNullable(is);
		return this;
	}

	public Optional<Double> getMultiplied() {
		return multiplied;
	}

	public ContextSpecification setMultiplied(Double multiplied) {
		this.multiplied = Optional.ofNullable(multiplied);
		return this;
	}

	public Optional<Double> getAdded() {
		return added;
	}

	public ContextSpecification setAdded(Double added) {
		this.added = Optional.ofNullable(added);
		return this;
	}

	public List<TimeSpecification> getDuring() {
		return during;
	}

	public ContextSpecification setDuring(List<TimeSpecification> during) {
		this.during = during;
		return this;
	}

}
