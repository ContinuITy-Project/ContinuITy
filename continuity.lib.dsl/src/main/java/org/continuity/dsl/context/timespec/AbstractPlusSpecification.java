package org.continuity.dsl.context.timespec;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.continuity.dsl.context.TimeSpecification;
import org.continuity.dsl.serialize.DurationToStringConverter;
import org.continuity.dsl.serialize.StringToDurationConverter;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class AbstractPlusSpecification implements TimeSpecification {

	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration duration;

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	@Override
	public boolean appliesToDate(Date date) {
		return true;
	}

	@Override
	public boolean appliesToNumerical(String variable, double value) {
		return true;
	}

	@Override
	public boolean appliesToBoolean(List<String> occurring) {
		return true;
	}

	@Override
	public boolean appliesToString(String variable, String value) {
		return true;
	}

	@Override
	public boolean hasPostprocessing() {
		return true;
	}

	@Override
	public List<Date> postprocess(List<Date> applied, Duration step) {
		postprocess(applied, this::addMissing, step);
		return applied;
	}

	@Override
	public Optional<QueryBuilder> toElasticQuery() {
		return Optional.empty();
	}

	@Override
	public boolean negateElasticQuery() {
		return false;
	}

	@Override
	public Optional<QueryBuilder> toPostprocessElasticQuery(List<Date> applied, Duration step) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		postprocess(applied, (ListIterator<Date> i, long l, long u, long s) -> addQuery(i, l, u, query), step);

		return Optional.of(query);
	}

	protected abstract void postprocess(List<Date> applied, PostprocessConsumer consumer, Duration step);

	protected abstract void addMissing(ListIterator<Date> iterator, long last, long until, long step);

	protected abstract void addQuery(ListIterator<Date> iterator, long last, long until, BoolQueryBuilder query);

	protected static interface PostprocessConsumer {

		void accept(ListIterator<Date> iterator, long last, long until, long step);

	}

}
