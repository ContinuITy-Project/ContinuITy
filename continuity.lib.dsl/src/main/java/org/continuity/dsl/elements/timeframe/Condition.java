package org.continuity.dsl.elements.timeframe;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.continuity.dsl.ContextValue;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a conditional expression based on one context variable.
 *
 * @author Henning Schulz
 *
 */
public class Condition {

	@JsonInclude(Include.NON_ABSENT)
	private Optional<ContextValue> is = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Double> greater = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Double> less = Optional.empty();

	public Optional<ContextValue> getIs() {
		return is;
	}

	public Condition setIs(ContextValue is) {
		this.is = Optional.ofNullable(is);
		return this;
	}

	public Optional<Double> getGreater() {
		return greater;
	}

	public Condition setGreater(Double greater) {
		this.greater = Optional.ofNullable(greater);
		return this;
	}

	public Optional<Double> getLess() {
		return less;
	}

	public Condition setLess(Double less) {
		this.less = Optional.ofNullable(less);
		return this;
	}

	public boolean appliesToNumerical(String variable, String checked, double value) {
		if (is.isPresent() && is.get().isNumeric()) {
			return !Objects.equals(variable, checked) || (is.get().getAsNumber() == value);
		} else {
			boolean isGreater = !greater.isPresent() || (greater.get() >= value);
			boolean isLess = !less.isPresent() || (less.get() <= value);

			return !Objects.equals(variable, checked) || (isGreater && isLess);
		}
	}

	public boolean appliesToBoolean(String variable, Set<String> occurring) {
		if (!is.isPresent() || !is.get().isBoolean()) {
			return true;
		}

		return is.get().getAsBoolean() == ((occurring != null) && occurring.contains(variable));
	}

	public boolean appliesToString(String variable, String checked, String value) {
		return !is.isPresent() || !is.get().isString() || !Objects.equals(variable, checked) || Objects.equals(is.get().getAsString(), value);
	}

	public QueryBuilder toElasticQuery(String variable) {
		if (is.isPresent() && !is.get().isNull()) {
			if (is.get().isString()) {
				return QueryBuilders.termQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_STRING).append(".").append(variable).toString(), is.get().getAsString());
			} else if (is.get().isNumeric()) {
				return QueryBuilders.termQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_NUMERIC).append(".").append(variable).toString(), is.get().getAsNumber());
			} else { // boolean
				return QueryBuilders.termQuery(IntensityRecord.PATH_CONTEXT_BOOLEAN, variable);
			}
		} else if (greater.isPresent() || less.isPresent()) {
			RangeQueryBuilder range = QueryBuilders.rangeQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_NUMERIC).append(".").append(variable).toString());

			if (greater.isPresent()) {
				range.gte(greater.get());
			}

			if (less.isPresent()) {
				range.lte(less.get());
			}

			return range;
		} else {
			return null;
		}
	}

	public boolean negateQuery() {
		return is.isPresent() && is.get().isBoolean() && (is.get().getAsBoolean() == false);
	}

}
