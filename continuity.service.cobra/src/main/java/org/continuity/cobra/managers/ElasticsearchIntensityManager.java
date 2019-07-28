package org.continuity.cobra.managers;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.session.SessionView;
import org.continuity.dsl.context.Context;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.idpa.AppId;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
public class ElasticsearchIntensityManager extends ElasticsearchScrollingManager<IntensityRecord> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchIntensityManager.class);

	private final ObjectMapper mapper;

	public ElasticsearchIntensityManager(String host, ObjectMapper mapper) throws IOException {
		super(host, "intensity");
		this.mapper = mapper;
	}

	/**
	 * Stores the passed intensity records for the given app-id, potentially overwriting old
	 * versions of the records.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param records
	 *            The intensity records to be stored.
	 * @throws IOException
	 */
	public void storeOrUpdateIntensities(AppId aid, List<String> tailoring, Collection<IntensityRecord> records) throws IOException {
		storeElements(aid, tailoring, records);
	}

	/**
	 * Reads the intensities between two dates.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param from
	 *            The lower bound.
	 * @param to
	 *            The upper bound.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readIntensitiesInRange(AppId aid, List<String> tailoring, Date from, Date to) throws IOException, TimeoutException {
		return readIntensitiesInRange(aid, tailoring, from.getTime(), to.getTime());
	}

	/**
	 * Reads the intensities between two dates.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param from
	 *            The lower bound as milliseconds.
	 * @param to
	 *            The upper bound as milliseconds.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readIntensitiesInRange(AppId aid, List<String> tailoring, long from, long to) throws IOException, TimeoutException {
		QueryBuilder query = QueryBuilders.rangeQuery(IntensityRecord.PATH_TIMESTAMP).from(from, true).to(to, true);
		return readElements(aid, tailoring, query, String.format("between %s and %s", formatOrNull(new Date(from)), formatOrNull(new Date(to))));
	}

	/**
	 * Reads the intensities defined by a {@link Context}.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param context
	 *            The context.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readIntensitiesInContext(AppId aid, List<String> tailoring, Context context) throws IOException, TimeoutException {
		return readElements(aid, tailoring, context.toElasticQuery(), "for passed context");
	}

	/**
	 * Reads the intensities described by the postprocessing of a {@link Context}.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param context
	 *            The context.
	 * @param applied
	 *            The dates that are selected.
	 * @param step
	 *            The minimum duration between two records.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readPostprocessing(AppId aid, List<String> tailoring, Context context, List<Date> applied, Duration step) throws IOException, TimeoutException {
		return readElements(aid, tailoring, context.toPostprocessingElasticQuery(applied, step), "for postprocessing of passed context");
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return new StringBuilder().append(aid.dropService()).append(".").append(tailoring).append(".intensity").toString();
	}

	@Override
	protected Pair<String, String> serialize(IntensityRecord intensity) {
		try {
			return Pair.of(mapper.writerWithView(SessionView.Extended.class).writeValueAsString(intensity), Long.toString(intensity.getTimestamp()));
		} catch (JsonProcessingException e) {
			LOGGER.error("Could not write TraceRecord to JSON string!", e);
			return null;
		}
	}

	@Override
	protected IntensityRecord deserialize(String json) {
		try {
			return mapper.readValue(json, IntensityRecord.class);
		} catch (IOException e) {
			LOGGER.error("Could not read IntensityRecord from JSON string!", e);
			return null;
		}
	}

}
