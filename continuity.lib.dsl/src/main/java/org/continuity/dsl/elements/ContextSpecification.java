package org.continuity.dsl.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.continuity.dsl.ContextValue;
import org.continuity.dsl.timeseries.ContextRecord;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.dsl.timeseries.NumericVariable;
import org.continuity.dsl.timeseries.StringVariable;

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

	public void adjustContext(List<IntensityRecord> intensities, String variable) {
		for (IntensityRecord record : intensities) {
			if (appliesTo(record)) {
				if (is.isPresent()) {
					if (is.get().isBoolean() && is.get().getAsBoolean()) {
						initContext(record, "boolean").getBoolean().add(variable);
					} else if (is.get().isNumeric()) {
						initContext(record, "numeric").getNumeric().add(new NumericVariable(variable, is.get().getAsNumber()));
					} else if (is.get().isString()) {
						initContext(record, "string").getString().add(new StringVariable(variable, is.get().getAsString()));
					}
				} else {
					Optional<NumericVariable> var = getNumericIfPresent(record).stream().filter(n -> n.getName().equals(variable)).findFirst();

					if (multiplied.isPresent() && var.isPresent()) {
						var.get().setValue(var.get().getValue() * multiplied.get());
					}

					if (added.isPresent() && var.isPresent()) {
						var.get().setValue(var.get().getValue() + added.get());
					} else if (added.isPresent() && !var.isPresent()) {
						initContext(record, "numeric").getNumeric().add(new NumericVariable(variable, added.get()));
					}
				}
			}
		}
	}

	private boolean appliesTo(IntensityRecord record) {
		return (during == null) || during.isEmpty() || during.stream().map(t -> t.appliesTo(record)).reduce(Boolean::logicalAnd).orElse(true);
	}

	private ContextRecord initContext(IntensityRecord record, String type) {
		ContextRecord context = record.getContext();

		if (context == null) {
			context = new ContextRecord();
			record.setContext(context);
		}

		switch (type) {
		case "boolean":
			Set<String> bool = context.getBoolean();

			if (bool == null) {
				bool = new HashSet<>();
				context.setBoolean(bool);
			}
			break;
		case "numeric":
			Set<NumericVariable> numeric = context.getNumeric();

			if (numeric == null) {
				numeric = new HashSet<>();
				context.setNumeric(numeric);
			}
			break;
		case "string":
			Set<StringVariable> string = context.getString();

			if (string == null) {
				string = new HashSet<>();
				context.setString(string);
			}
			break;
		}

		return context;
	}

	private Set<NumericVariable> getNumericIfPresent(IntensityRecord record) {
		if ((record.getContext() == null) || (record.getContext().getNumeric() == null)) {
			return Collections.emptySet();
		} else {
			return record.getContext().getNumeric();
		}
	}

}
