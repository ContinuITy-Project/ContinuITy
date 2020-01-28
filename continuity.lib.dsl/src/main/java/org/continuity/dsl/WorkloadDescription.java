package org.continuity.dsl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.continuity.dsl.elements.ContextSpecification;
import org.continuity.dsl.elements.TimeSpecification;
import org.continuity.dsl.elements.TypedProperties;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Description of the workload to be used in the generated load test.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "timeframe", "context", "aggregation", "adjustments" })
public class WorkloadDescription {

	private static final Duration DEFAULT_DURATION = Duration.of(365, ChronoUnit.DAYS);

	@JsonIgnore
	private LocalDateTime now;

	@JsonInclude(Include.NON_EMPTY)
	private List<TimeSpecification> timeframe;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, List<ContextSpecification>> context;

	@JsonInclude(Include.NON_NULL)
	private TypedProperties aggregation;

	@JsonInclude(Include.NON_EMPTY)
	private List<TypedProperties> adjustments;

	public WorkloadDescription() {
		this.now = LocalDateTime.now();
	}

	public List<TimeSpecification> getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(List<TimeSpecification> timeframe) {
		this.timeframe = timeframe;
	}

	public Map<String, List<ContextSpecification>> getContext() {
		return context;
	}

	public void setContext(Map<String, List<ContextSpecification>> context) {
		this.context = context;
	}

	public TypedProperties getAggregation() {
		return aggregation;
	}

	public void setAggregation(TypedProperties aggregation) {
		this.aggregation = aggregation;
	}

	public List<TypedProperties> getAdjustments() {
		return adjustments;
	}

	public void setAdjustments(List<TypedProperties> adjustments) {
		this.adjustments = adjustments;
	}

	/**
	 * Transforms the {@code timeframe} specification to an elasticsearch query.
	 *
	 * @param timeZone
	 *            The time zone.
	 *
	 * @return The query.
	 */
	public QueryBuilder toElasticQuery(ZoneId timeZone) {
		setDefaultDates();

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		timeframe.stream().map(ts -> ts.toElasticQuery(timeZone)).flatMap(List::stream).forEach(p -> {
			if (p.getRight()) {
				query.must(p.getLeft());
			} else {
				query.mustNot(p.getLeft());
			}
		});

		return query;
	}

	public boolean requiresPostprocessing() {
		return timeframe.stream().map(TimeSpecification::requiresPostprocessing).reduce(Boolean::logicalOr).orElse(false);
	}

	/**
	 * Transforms the {@code when} specification to an elasticsearch query that should be submitted
	 * based on the initially retrieved dates.
	 *
	 * @param applied
	 *            The initially retrieved dates.
	 * @param step
	 *            The duration between two dates.
	 * @return The query.
	 */
	public QueryBuilder toPostprocessingElasticQuery(List<LocalDateTime> applied, Duration step) {
		setDefaultDates();

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		for (TimeSpecification timespec : timeframe) {
			Optional<QueryBuilder> subQuery = timespec.toPostprocessElasticQuery(applied, step);

			if (subQuery.isPresent()) {
				query.should(subQuery.get());
			}
		}

		return query;
	}

	/**
	 * Gets the minimum date specified.
	 *
	 * @return
	 */
	@JsonIgnore
	public LocalDateTime getMinDate() {
		setDefaultDates();

		LocalDateTime minDate = timeframe.stream().map(TimeSpecification::getMinDate).filter(Optional::isPresent).map(Optional::get).reduce((a, b) -> a.isBefore(b) ? a : b).orElse(now);

		Duration addition = timeframe.stream().map(TimeSpecification::getMaxBeginningAddition).filter(Optional::isPresent).map(Optional::get).reduce((a, b) -> a.minus(b).toMillis() > 0 ? a : b)
				.orElse(Duration.ZERO);

		return minDate.minus(addition);
	}

	/**
	 * Gets the maximum date specified.
	 *
	 * @return
	 */
	@JsonIgnore
	public LocalDateTime getMaxDate() {
		setDefaultDates();

		LocalDateTime minDate = timeframe.stream().map(TimeSpecification::getMaxDate).filter(Optional::isPresent).map(Optional::get).reduce((a, b) -> a.isAfter(b) ? a : b)
				.orElse(now.plus(DEFAULT_DURATION));

		Duration addition = timeframe.stream().map(TimeSpecification::getMaxEndAddition).filter(Optional::isPresent).map(Optional::get).reduce((a, b) -> a.minus(b).toMillis() > 0 ? a : b)
				.orElse(Duration.ZERO);

		return minDate.plus(addition);
	}

	private void setDefaultDates() {
		for (TimeSpecification spec : timeframe) {
			spec.setDefaultMinDate(now);
			spec.setDefaultMaxDate(now.plus(DEFAULT_DURATION));
		}
	}

	/**
	 * Adjusts the context as specified in {@link #context}.
	 *
	 * @param intensities
	 *            The intensity record to adjust.
	 * @param timeZone
	 *            The time zone in which the timestamps should be evaluated.
	 */
	public void adjustContext(List<IntensityRecord> intensities, ZoneId timeZone) {
		if (context == null) {
			return;
		}

		for (Entry<String, List<ContextSpecification>> entry : context.entrySet()) {
			for (ContextSpecification spec : entry.getValue()) {
				spec.adjustContext(intensities, timeZone, entry.getKey());
			}
		}
	}

	public void setNow(LocalDateTime now) {
		this.now = now;
	}

}
