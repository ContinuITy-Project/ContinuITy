package org.continuity.dsl.context.timespec;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.continuity.dsl.timeseries.IntensityRecord;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Adds a certain time to all detected periods.
 *
 * @author Henning Schulz
 *
 */
public class PlusSpecification extends AbstractPlusSpecification {

	@Override
	protected void postprocess(List<Date> applied, PostprocessConsumer consumer, Duration step) {
		ListIterator<Date> iterator = applied.listIterator();
		long stepMillis = step.getSeconds() * 1000;
		long durationMillis = getDuration().getSeconds() * 1000;
		Date last = null;

		while (iterator.hasNext()) {
			Date next = iterator.next();

			if ((last != null) && ((next.getTime() - last.getTime()) > stepMillis)) {
				iterator.previous();
				consumer.accept(iterator, last.getTime(), Math.min(last.getTime() + durationMillis, next.getTime()), stepMillis);
				iterator.next();
			}

			last = next;
		}
	}

	@Override
	protected void addMissing(ListIterator<Date> iterator, long last, long until, long step) {
		long next = last + step;

		while (next < until) {
			iterator.add(new Date(next));

			next += step;
		}
	}

	@Override
	protected void addQuery(ListIterator<Date> iterator, long last, long until, BoolQueryBuilder query) {
		query.should(QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP).gt(last).lte(until));
	}

}
