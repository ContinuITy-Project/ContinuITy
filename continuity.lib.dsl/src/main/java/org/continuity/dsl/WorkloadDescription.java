package org.continuity.dsl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.continuity.dsl.elements.ContextSpecification;
import org.continuity.dsl.elements.TimeSpecification;
import org.continuity.dsl.elements.TypedProperties;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

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

	@JsonInclude(Include.NON_EMPTY)
	private List<TimeSpecification> timeframe;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, List<ContextSpecification>> context;

	@JsonInclude(Include.NON_NULL)
	private TypedProperties aggregation;

	@JsonInclude(Include.NON_EMPTY)
	private List<TypedProperties> adjustments;

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
	 * @return The query.
	 */
	public QueryBuilder toElasticQuery() {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		timeframe.stream().map(TimeSpecification::toElasticQuery).flatMap(List::stream).forEach(p -> {
			if (p.getRight()) {
				query.must(p.getLeft());
			} else {
				query.mustNot(p.getLeft());
			}
		});

		return query;
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
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		for (TimeSpecification timespec : timeframe) {
			Optional<QueryBuilder> subQuery = timespec.toPostprocessElasticQuery(applied, step);

			if (subQuery.isPresent()) {
				query.should(subQuery.get());
			}
		}

		return query;
	}

}
