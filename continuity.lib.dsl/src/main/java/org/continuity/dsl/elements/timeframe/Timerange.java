package org.continuity.dsl.elements.timeframe;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.dsl.elements.TimeSpecification;
import org.continuity.dsl.timeseries.IntensityRecord;
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
	private LocalDateTime now;

	@JsonInclude(Include.NON_ABSENT)
	private Optional<LocalDateTime> from = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<LocalDateTime> to = Optional.empty();

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Duration> duration = Optional.empty();

	public Timerange() {
		this.now = LocalDateTime.now();
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
	public boolean appliesToDate(LocalDateTime date) {
		LocalDateTime from = this.from.orElse(now);
		Duration duration = this.duration.orElse(Duration.of(1, ChronoUnit.YEARS));
		LocalDateTime to = this.to.orElse(now.plus(Duration.of(1, ChronoUnit.YEARS)));

		return !from.isAfter(date) && !to.isBefore(date) && !from.plus(duration).isBefore(date);
	}

	@Override
	public List<Pair<QueryBuilder, Boolean>> toElasticQuery() {
		LocalDateTime from = this.from.orElse(now);
		Duration duration = this.duration.orElse(Duration.of(1, ChronoUnit.YEARS));
		LocalDateTime to = this.to.orElse(now.plus(Duration.of(1, ChronoUnit.YEARS)));

		if (from.plus(duration).isBefore(to)) {
			to = from.plus(duration);
		}

		return Collections.singletonList(Pair.of(QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP).gte(toMillis(from)).lte(toMillis(to)), true));
	}

	private long toMillis(LocalDateTime date) {
		return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

}
