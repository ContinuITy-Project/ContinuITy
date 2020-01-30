package org.continuity.dsl.elements.timeframe;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.continuity.dsl.ContextValue;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.dsl.timeseries.NumericVariable;
import org.continuity.dsl.timeseries.StringVariable;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

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

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Boolean> exists = Optional.empty();

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

	public Optional<Boolean> getExists() {
		return exists;
	}

	public void setExists(Boolean exists) {
		this.exists = Optional.ofNullable(exists);
	}

	public boolean appliesToNumerical(String variable, Set<NumericVariable> checked) {
		if (is.isPresent() && is.get().isNumeric()) {
			if (is.get().getAsNumber() == 0) {
				return (checked == null) || (checked.stream().filter(v -> variable.equals(v.getName())).filter(v -> v.getValue() != 0).count() == 0);
			} else {
				return (checked != null) && (checked.stream().filter(v -> variable.equals(v.getName())).filter(v -> is.get().getAsNumber() == v.getValue()).count() > 0);
			}
		} else {
			if (checked == null) {
				checked = Collections.emptySet();
			}

			boolean isGreater = !greater.isPresent()
					|| checked.stream().filter(v -> variable.equals(v.getName())).map(v -> v.getValue() >= greater.get()).reduce(Boolean::logicalAnd).orElse(greater.get() <= 0);
			boolean isLess = !less.isPresent() || checked.stream().filter(v -> variable.equals(v.getName())).map(v -> v.getValue() <= less.get()).reduce(Boolean::logicalAnd).orElse(less.get() >= 0);

			return isGreater && isLess;
		}
	}

	public boolean appliesToBoolean(String variable, Set<String> occurring) {
		if (!is.isPresent() || !is.get().isBoolean()) {
			return true;
		}

		return is.get().getAsBoolean() == ((occurring != null) && occurring.contains(variable));
	}

	public boolean appliesToString(String variable, Set<StringVariable> checked) {
		boolean appliesToIs = !is.isPresent() || !is.get().isString()
				|| ((checked != null) && (checked.stream().filter(v -> variable.equals(v.getName())).filter(v -> is.get().getAsString().equals(v.getValue())).count() > 0));

		boolean appliesToExists = !exists.isPresent() || (((checked != null) && (checked.stream().map(StringVariable::getName).filter(variable::equals).count() > 0)) == exists.get());

		return appliesToIs && appliesToExists;
	}

	public QueryBuilder toElasticQuery(String variable) {
		if (is.isPresent() && !is.get().isNull()) {
			if (is.get().isString()) {
				return QueryBuilders.termQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_STRING).append(".").append(variable).toString(), is.get().getAsString());
			} else if (is.get().isNumeric()) {
				String field = new StringBuilder().append(IntensityRecord.PATH_CONTEXT_NUMERIC).append(".").append(variable).toString();
				TermQueryBuilder term = QueryBuilders.termQuery(field, is.get().getAsNumber());
				return (is.get().getAsNumber() == 0) ? wrapWithExists(field, term) : term;
			} else { // boolean
				return QueryBuilders.termQuery(IntensityRecord.PATH_CONTEXT_BOOLEAN, variable);
			}
		} else if (greater.isPresent() || less.isPresent()) {
			String field = new StringBuilder().append(IntensityRecord.PATH_CONTEXT_NUMERIC).append(".").append(variable).toString();
			RangeQueryBuilder range = QueryBuilders.rangeQuery(field);

			if (greater.isPresent()) {
				range.gte(greater.get());
			}

			if (less.isPresent()) {
				range.lte(less.get());
			}

			return ((greater.orElse(0.0) <= 0) && (less.orElse(0.0) >= 0)) ? wrapWithExists(field, range) : range;
		} else if (exists.isPresent()) {
			return QueryBuilders.existsQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_STRING).append(".").append(variable).toString());
		} else {
			return null;
		}
	}

	private BoolQueryBuilder wrapWithExists(String field, AbstractQueryBuilder<?> inner) {
		return QueryBuilders.boolQuery().should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(field))).should(inner);
	}

	public boolean negateQuery() {
		return (exists.isPresent() && !exists.get()) || (is.isPresent() && is.get().isBoolean() && (is.get().getAsBoolean() == false));
	}

}
