package org.continuity.dsl.context.timespec;

import java.util.Date;
import java.util.Optional;

import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Applies if the date is after the specified one.
 *
 * @author Henning Schulz
 *
 */
public class AfterSpecification extends AbstractDateSpecification {

	@Override
	public boolean appliesToDate(Date date) {
		return (date == null) || !date.before(this.getDate());
	}

	@Override
	public Optional<QueryBuilder> toElasticQuery() {
		return Optional.of(QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP).gte(this.getDate().getTime()));
	}

}
