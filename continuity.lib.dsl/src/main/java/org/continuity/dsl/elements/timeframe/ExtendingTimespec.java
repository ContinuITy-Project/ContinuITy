package org.continuity.dsl.elements.timeframe;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.dsl.elements.TimeSpecification;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Extends the selected time frame.
 *
 * @author Henning Schulz
 *
 */
public class ExtendingTimespec implements TimeSpecification {

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Duration> beginning;

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Duration> end;

	public Optional<Duration> getBeginning() {
		return beginning;
	}

	public ExtendingTimespec setBeginning(Duration before) {
		this.beginning = Optional.ofNullable(before);
		return this;
	}

	public Optional<Duration> getEnd() {
		return end;
	}

	public ExtendingTimespec setEnd(Duration after) {
		this.end = Optional.ofNullable(after);
		return this;
	}

	@Override
	public boolean appliesToDate(LocalDateTime date) {
		return true;
	}

	@Override
	public boolean appliesToNumerical(String variable, double value) {
		return true;
	}

	@Override
	public boolean appliesToBoolean(Set<String> occurring) {
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
	public List<LocalDateTime> postprocess(List<LocalDateTime> applied, Duration step) {
		postprocessAfter(applied, this::addMissing, step);
		postprocessBefore(applied, this::addMissing, step);
		return applied;
	}

	@Override
	public Optional<QueryBuilder> toPostprocessElasticQuery(List<LocalDateTime> applied, Duration step) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		postprocessAfter(applied, (Consumer<LocalDateTime> a, LocalDateTime f, LocalDateTime u, Duration s) -> addQuery(query, f, u), step);

		return Optional.of(query);
	}

	@Override
	public List<Pair<QueryBuilder, Boolean>> toElasticQuery() {
		return Collections.emptyList();
	}

	@Override
	public boolean requiresPostprocessing() {
		return true;
	}

	private void postprocessBefore(List<LocalDateTime> applied, PostprocessConsumer consumer, Duration step) {
		if (!beginning.isPresent() || beginning.get().isZero() || beginning.get().isNegative()) {
			return;
		}

		ListIterator<LocalDateTime> iterator = applied.listIterator(applied.size());
		LocalDateTime last = null;
		Duration negStep = Duration.ZERO.minus(step);

		while (iterator.hasPrevious()) {
			LocalDateTime next = iterator.previous();

			if ((last != null) && (Duration.between(next, last).minus(step).toMillis() > 0)) {
				iterator.next();

				LocalDateTime until = last.minus(beginning.get());
				consumer.accept(d -> {
					iterator.add(d);
					iterator.previous();
				}, last, next.isAfter(until) ? next : until, negStep);

				iterator.previous();
			}

			last = next;
		}
	}

	private void postprocessAfter(List<LocalDateTime> applied, PostprocessConsumer consumer, Duration step) {
		if (!end.isPresent() || end.get().isZero() || end.get().isNegative()) {
			return;
		}

		ListIterator<LocalDateTime> iterator = applied.listIterator();
		LocalDateTime last = null;

		while (iterator.hasNext()) {
			LocalDateTime next = iterator.next();

			if ((last != null) && (Duration.between(last, next).minus(step).toMillis() > 0)) {
				iterator.previous();

				LocalDateTime until = last.plus(end.get());
				consumer.accept(iterator::add, last, next.isBefore(until) ? next : until, step);

				iterator.next();
			}

			last = next;
		}
	}

	private void addMissing(Consumer<LocalDateTime> adder, LocalDateTime from, LocalDateTime until, Duration step) {
		LocalDateTime next = from.plus(step);

		while (step.isNegative() ? next.isAfter(until) : next.isBefore(until)) {
			adder.accept(next);

			next = next.plus(step);
		}
	}

	private void addQuery(BoolQueryBuilder query, LocalDateTime from, LocalDateTime until) {
		RangeQueryBuilder range = QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP);

		long fromMillis = from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long untilMillis = until.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

		if (from.isBefore(until)) {
			range.gt(fromMillis).lte(untilMillis);
		} else {
			range.lt(fromMillis).gte(untilMillis);
		}

		query.should(range);
	}

	@Override
	public Optional<Duration> getMaxBeginningAddition() {
		return end;
	}

	@Override
	public Optional<Duration> getMaxEndAddition() {
		return beginning;
	}

	protected static interface PostprocessConsumer {

		void accept(Consumer<LocalDateTime> adder, LocalDateTime from, LocalDateTime until, Duration step);

	}

}
