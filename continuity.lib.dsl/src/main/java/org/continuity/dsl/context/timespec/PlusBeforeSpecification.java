package org.continuity.dsl.context.timespec;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Adds a certain time to the beginning of all detected periods.
 *
 * @author Henning Schulz
 *
 */
public class PlusBeforeSpecification extends AbstractPlusSpecification {

	@Override
	protected void postprocess(List<Date> applied, PostprocessConsumer consumer, Duration step) {
		ListIterator<Date> iterator = applied.listIterator(applied.size());
		long stepMillis = step.getSeconds() * 1000;
		long durationMillis = getDuration().getSeconds() * 1000;
		Date last = null;

		while (iterator.hasPrevious()) {
			Date next = iterator.previous();

			if ((last != null) && ((last.getTime() - next.getTime()) > stepMillis)) {
				iterator.next();
				consumer.accept(iterator, last.getTime(), Math.max(last.getTime() - durationMillis, next.getTime()), stepMillis);
				iterator.previous();
			}

			last = next;
		}
	}

	@Override
	protected void addMissing(ListIterator<Date> iterator, long last, long until, long step) {
		long next = last - step;

		while (next > until) {
			iterator.add(new Date(next));
			iterator.previous();

			next -= step;
		}
	}

	@Override
	protected void addQuery(ListIterator<Date> iterator, long last, long until, BoolQueryBuilder query) {
		query.should(QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP).lt(last).lte(until));
	}

}
