package org.continuity.dsl.context.timespec;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.continuity.dsl.StringOrNumeric;
import org.continuity.dsl.context.TimeSpecification;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "what", "to" })
public abstract class AbstractEqualitySpecification implements TimeSpecification {

	private String what;

	private StringOrNumeric to;

	public String getWhat() {
		return what;
	}

	public void setWhat(String what) {
		this.what = what;
	}

	public StringOrNumeric getTo() {
		return to;
	}

	public void setTo(StringOrNumeric to) {
		this.to = to;
	}

	@Override
	public boolean appliesToDate(Date date) {
		return true;
	}

	@Override
	public boolean appliesToNumerical(String variable, double value) {
		if (!this.to.isNumeric() || !Objects.equals(this.what, variable)) {
			return true;
		}

		double number = this.to.getAsNumber();

		return negateElasticQuery() != ((Math.abs(value - number) / number) < 0.001);
	}

	@Override
	public boolean appliesToBoolean(List<String> occurring) {
		return true;
	}

	@Override
	public boolean appliesToString(String variable, String value) {
		return !Objects.equals(this.what, variable) || !this.to.isString() || (negateElasticQuery() != Objects.equals(this.to.getAsString(), value));
	}

	@Override
	public Optional<QueryBuilder> toElasticQuery() {
		if (to.isNumeric()) {
			return Optional.of(QueryBuilders.termQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_NUMERIC).append(".").append(what).toString(), to.getAsNumber()));
		} else if (to.isString()) {
			return Optional.of(QueryBuilders.termQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_STRING).append(".").append(what).toString(), to.getAsString()));
		} else {
			return Optional.empty();
		}
	}

}
