package org.continuity.dsl.context.timespec;

import java.util.Objects;
import java.util.Optional;

import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Applies if the specified numerical variable is less than the specified value.
 *
 * @author Henning Schulz
 *
 */
public class LessSpecification extends AbstractComparingSpecification {

	@Override
	public boolean appliesToNumerical(String variable, double value) {
		return !Objects.equals(getWhat(), variable) || (value <= getThan());
	}

	@Override
	public Optional<QueryBuilder> toElasticQuery() {
		return Optional.of(QueryBuilders.rangeQuery(new StringBuilder().append(IntensityRecord.PATH_CONTEXT_NUMERIC).append(".").append(getWhat()).toString()).lte(getThan()));
	}

}
