package org.continuity.lctl.elements.timeframe;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.lctl.elements.TimeSpecification;
import org.continuity.lctl.timeseries.IntensityRecord;
import org.continuity.lctl.timeseries.NumericVariable;
import org.continuity.lctl.timeseries.StringVariable;
import org.continuity.lctl.utils.DateUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Applies if the date is inside a time range.
 *
 * @author Henning Schulz
 *
 */
public class Timerange implements TimeSpecification {

	@JsonIgnore
	private LocalDateTime defaultFrom;

	@JsonIgnore
	private LocalDateTime defaultTo;

	@JsonInclude(Include.NON_ABSENT)
	private Optional<LocalDateTime> from = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<LocalDateTime> to = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Duration> duration = Optional.empty();

	@Override
	public void setDefaultMinDate(LocalDateTime min) {
		this.defaultFrom = min;
	}

	@Override
	public void setDefaultMaxDate(LocalDateTime max) {
		this.defaultTo = max;
	}

	public Optional<LocalDateTime> getFrom() {
		return from;
	}

	public Timerange setFrom(LocalDateTime from) {
		this.from = Optional.ofNullable(from);
		return this;
	}

	public Optional<LocalDateTime> getTo() {
		return to;
	}

	public Timerange setTo(LocalDateTime to) {
		this.to = Optional.ofNullable(to);
		return this;
	}

	public Optional<Duration> getDuration() {
		return duration;
	}

	public Timerange setDuration(Duration duration) {
		this.duration = Optional.ofNullable(duration);
		return this;
	}

	@Override
	public boolean appliesToNumerical(Set<NumericVariable> variables) {
		return true;
	}

	@Override
	public boolean appliesToBoolean(Set<String> occurring) {
		return true;
	}

	@Override
	public boolean appliesToString(Set<StringVariable> variables) {
		return true;
	}

	@Override
	public boolean appliesToDate(LocalDateTime date) {
		LocalDateTime from = effectiveFrom();
		LocalDateTime to = effectiveTo();

		return !from.isAfter(date) && !to.isBefore(date);
	}

	@Override
	public List<Pair<QueryBuilder, Boolean>> toElasticQuery(ZoneId timeZone) {
		return Collections.singletonList(
				Pair.of(QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP).gte(DateUtils.toEpochMillis(effectiveFrom(), timeZone)).lte(DateUtils.toEpochMillis(effectiveTo(), timeZone)), true));
	}

	@Override
	public Optional<LocalDateTime> getMaxDate() {
		return Optional.of(effectiveTo());
	}

	@Override
	public Optional<LocalDateTime> getMinDate() {
		return Optional.of(effectiveFrom());
	}

	private LocalDateTime effectiveFrom() {
		return this.from.orElse(defaultFrom);
	}

	private LocalDateTime effectiveTo() {
		LocalDateTime from = effectiveFrom();

		if (this.to.isPresent()) {
			return this.to.get();
		} else if (this.duration.isPresent()) {
			return from.plus(this.duration.get());
		} else {
			return defaultTo;
		}
	}

}
