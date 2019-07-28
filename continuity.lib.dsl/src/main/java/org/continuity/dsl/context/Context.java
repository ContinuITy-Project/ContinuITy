package org.continuity.dsl.context;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Describes the workload context of a load test to be generated. Includes the time range to be
 * considered for generation ({@code when}), the variables influencing the workload
 * ({@code influencing}), and adjustments to the generated workload ({@code adjusted}).
 *
 * @author Henning Schulz
 *
 */
public class Context {

	@JsonInclude(Include.NON_EMPTY)
	private List<TimeSpecification> when;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, List<WorkloadInfluence>> influencing;

	@JsonInclude(Include.NON_EMPTY)
	private List<WorkloadAdjustment> adjusted;

	public List<TimeSpecification> getWhen() {
		return when;
	}

	public void setWhen(List<TimeSpecification> when) {
		this.when = when;
	}

	public Map<String, List<WorkloadInfluence>> getInfluencing() {
		return influencing;
	}

	public void setInfluencing(Map<String, List<WorkloadInfluence>> influencing) {
		this.influencing = influencing;
	}

	public List<WorkloadAdjustment> getAdjusted() {
		return adjusted;
	}

	public void setAdjusted(List<WorkloadAdjustment> adjusted) {
		this.adjusted = adjusted;
	}

	/**
	 * Transforms the {@code when} specification to an elasticsearch query.
	 *
	 * @return The query.
	 */
	public QueryBuilder toElasticQuery() {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		for (TimeSpecification timespec : when) {
			Optional<QueryBuilder> subQuery = timespec.toElasticQuery();

			if (subQuery.isPresent()) {
				if (timespec.negateElasticQuery()) {
					query.mustNot(subQuery.get());
				} else {
					query.must(subQuery.get());
				}
			}
		}

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
	public QueryBuilder toPostprocessingElasticQuery(List<Date> applied, Duration step) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		for (TimeSpecification timespec : when) {
			Optional<QueryBuilder> subQuery = timespec.toPostprocessElasticQuery(applied, step);

			if (subQuery.isPresent()) {
				query.should(subQuery.get());
			}
		}

		return query;
	}

}
