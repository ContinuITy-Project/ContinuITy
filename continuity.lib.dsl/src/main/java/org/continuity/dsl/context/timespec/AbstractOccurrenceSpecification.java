package org.continuity.dsl.context.timespec;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.continuity.dsl.context.TimeSpecification;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class AbstractOccurrenceSpecification implements TimeSpecification {

	private String what;

	public String getWhat() {
		return what;
	}

	public void setWhat(String what) {
		this.what = what;
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
		return negateElasticQuery() != ((occurring != null) && occurring.contains(this.what));
	}

	@Override
	public boolean appliesToString(String variable, String value) {
		return true;
	}

	@Override
	public Optional<QueryBuilder> toElasticQuery() {
		return Optional.of(QueryBuilders.termQuery(IntensityRecord.PATH_CONTEXT_BOOLEAN, getWhat()));
	}

}
