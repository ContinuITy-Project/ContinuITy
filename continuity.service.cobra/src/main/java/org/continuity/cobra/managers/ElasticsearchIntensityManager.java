package org.continuity.cobra.managers;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.continuity.api.entities.artifact.session.Session;
import org.continuity.idpa.AppId;
import org.continuity.lctl.WorkloadDescription;
import org.continuity.lctl.timeseries.IntensityRecord;
import org.continuity.lctl.utils.DateUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
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

	private static final String UPDATE_SCRIPT_ID = "update-intensity";

	private static final String UPDATE_LEGACY_SCRIPT_ID = "update-intensity-from-legacy";

	private static final String REMOVE_CONTEXT_SCRIPT_ID = "remove-context";

	private final ObjectMapper mapper;

	private boolean updateScriptInitialized = false;

	private boolean updateLegacyScriptInitialized = false;

	private boolean removeContextScriptInitialized = false;

	public ElasticsearchIntensityManager(String host, ObjectMapper mapper, int bulkTimeoutSeconds) throws IOException {
		super(host, "intensity", bulkTimeoutSeconds);
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
		boolean containsContexts = records.stream().map(IntensityRecord::getContext).filter(c -> (c != null) && !c.isEmpty()).count() > 0;

		if (containsContexts) {
			try {
				if (!updateScriptInitialized) {
					updateScriptInitialized = initUpdateScript(UPDATE_SCRIPT_ID);
				}
			} catch (Exception e) {
				LOGGER.error("Could not initialize update script! Hoping it is already present...", e);
			}
			storeOrUpdateByScript(aid, tailoring, records, this::createUpdateScript);
		} else {
			storeOrUpdateElements(aid, tailoring, records);
		}
	}

	private Script createUpdateScript(IntensityRecord record) {
		Map<String, Object> params = new HashMap<>();

		if ((record.getIntensity() != null) && !record.getIntensity().isEmpty()) {
			params.put("intensity", record.getIntensity());
		}

		if (record.getContext() != null) {
			if ((record.getContext().getNumeric() != null) && !record.getContext().getNumeric().isEmpty()) {
				params.put("numeric", record.getContext().getNumeric());
			}

			if ((record.getContext().getString() != null) && !record.getContext().getString().isEmpty()) {
				params.put("string", record.getContext().getString());
			}

			if ((record.getContext().getBoolean() != null) && !record.getContext().getBoolean().isEmpty()) {
				params.put("boolean", record.getContext().getBoolean());
			}
		}

		return new Script(ScriptType.STORED, null, UPDATE_SCRIPT_ID, params);
	}

	/**
	 * Fills the intensities in the given time range. Will wait until the data can be queried.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param from
	 *            The lower bound of the time range.
	 * @param to
	 *            The upper bound of the time range.
	 * @param resolution
	 *            The step width between two intensity records.
	 * @param timeZone
	 *            The time zone of the dates.
	 * @throws IOException
	 */
	public void fillIntensities(AppId aid, List<String> tailoring, LocalDateTime from, LocalDateTime to, Duration resolution, ZoneId timeZone) throws IOException {
		LocalDateTime latest = DateUtils.fromEpochMillis(getLatestDate(aid, tailoring).getTime(), timeZone);

		if (latest.isAfter(to)) {
			LOGGER.info("All required intensity records are already present.");
			return;
		}

		if (latest.isAfter(from)) {
			from = latest.plus(resolution);
		}

		long numIntensities = ((DateUtils.toEpochMillis(to, timeZone) - DateUtils.toEpochMillis(from, timeZone)) / resolution.toMillis()) + 1;
		LOGGER.info("Inserting {} empty intensity records...", numIntensities);

		List<IntensityRecord> intensities = Stream.iterate(from, d -> d.plus(resolution)).limit(numIntensities).map(d -> DateUtils.toEpochMillis(d, timeZone)).map(IntensityRecord::new)
				.collect(Collectors.toList());

		storeElementsIfAbsent(aid, tailoring, intensities, true);
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
	 * @param timeZone
	 *            The time zone of the dates.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readIntensitiesInRange(AppId aid, List<String> tailoring, LocalDateTime from, LocalDateTime to, ZoneId timeZone) throws IOException, TimeoutException {
		return readIntensitiesInRange(aid, tailoring, DateUtils.toEpochMillis(from, timeZone), DateUtils.toEpochMillis(to, timeZone));
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
		FieldSortBuilder sort = new FieldSortBuilder("timestamp").order(SortOrder.ASC);
		return readElements(aid, tailoring, query, sort, DEFAULT_SCROLL_SIZE, TOTAL_SIZE_ALL, String.format("between %s and %s", formatOrNull(new Date(from)), formatOrNull(new Date(to))));
	}

	/**
	 * Reads the intensities defined by a {@link WorkloadDescription}.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param workloadDescription
	 *            The workload description.
	 * @param timeZone
	 *            The time zone of the dates.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readDescribedIntensities(AppId aid, List<String> tailoring, WorkloadDescription workloadDescription, ZoneId timeZone) throws IOException, TimeoutException {
		FieldSortBuilder sort = new FieldSortBuilder("timestamp").order(SortOrder.ASC);
		return readElements(aid, tailoring, workloadDescription.toElasticQuery(timeZone), sort, DEFAULT_SCROLL_SIZE, TOTAL_SIZE_ALL, "for passed workload description");
	}

	/**
	 * Reads the intensities described by the postprocessing of a {@link WorkloadDescription}.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param workloadDescription
	 *            The workload description.
	 * @param applied
	 *            The dates that are selected.
	 * @param step
	 *            The minimum duration between two records.
	 * @return The found intensities.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<IntensityRecord> readPostprocessing(AppId aid, List<String> tailoring, WorkloadDescription workloadDescription, List<LocalDateTime> applied, Duration step)
			throws IOException, TimeoutException {
		return readElements(aid, tailoring, workloadDescription.toPostprocessingElasticQuery(applied, step), "for postprocessing of passed workload description");
	}

	/**
	 * Gets the earliest date occurring in the stored intensities.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The list of services to which the intensities belong. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @return The found date. In case no sessions could be found, 292278994-08-17 08:12:55 (max
	 *         value of long) will be returned.
	 * @throws IOException
	 */
	public Date getEarliestDate(AppId aid, List<String> tailoring) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return new Date(Long.MAX_VALUE);
		}

		SearchSourceBuilder source = new SearchSourceBuilder();
		source.aggregation(AggregationBuilders.min("min_timestamp").field("timestamp").missing(Long.MAX_VALUE));

		SearchRequest search = new SearchRequest(index).source(source);

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", aid, index, e.getMessage());
			return new Date(Long.MAX_VALUE);
		}

		ParsedMin min = response.getAggregations().get("min_timestamp");
		double millis = min.getValue();

		return new Date(Math.round(millis));
	}

	/**
	 * Gets the latest date occurring in the stored intensities.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The list of services to which the intensities belong. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @return The found date. In case no sessions could be found, 1970-01-01 01:00:00 will be
	 *         returned.
	 * @throws IOException
	 */
	public Date getLatestDate(AppId aid, List<String> tailoring) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return new Date(Long.MAX_VALUE);
		}

		SearchSourceBuilder source = new SearchSourceBuilder();
		source.aggregation(AggregationBuilders.max("max_timestamp").field("timestamp").missing(0));

		SearchRequest search = new SearchRequest(index).source(source);

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", aid, index, e.getMessage());
			return new Date(Long.MAX_VALUE);
		}

		ParsedMax max = response.getAggregations().get("max_timestamp");
		double millis = max.getValue();

		return new Date(Math.round(millis));
	}

	/**
	 * Updates the stored intensity documents from the legacy structure to the new one.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The list of services to which the intensities belong. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @throws IOException
	 */
	public void updateIndexFromLegacy(AppId aid, List<String> tailoring) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			LOGGER.warn("Cannot update index {}. It does not exist!", index);
			return;
		}

		try {
			if (!updateLegacyScriptInitialized) {
				updateLegacyScriptInitialized = initUpdateScript(UPDATE_LEGACY_SCRIPT_ID);
			}
		} catch (Exception e) {
			LOGGER.error("Could not initialize update script! Hoping it is already present...", e);
		}

		LOGGER.info("Updating mapping of index {} from legacy...", index);
		updateMapping(index);

		LOGGER.info("Updating index {} from legacy...", index);

		updateByQueryAsync(index, REMOVE_CONTEXT_SCRIPT_ID);
	}

	/**
	 * Removes all context fields from the intensity documents.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The list of services to which the intensities belong. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @throws IOException
	 */
	public void clearContext(AppId aid, List<String> tailoring) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			LOGGER.warn("Cannot clear context in index {}. It does not exist!", index);
			return;
		}

		try {
			if (!removeContextScriptInitialized) {
				removeContextScriptInitialized = initUpdateScript(REMOVE_CONTEXT_SCRIPT_ID);
			}
		} catch (Exception e) {
			LOGGER.error("Could not initialize removal script! Hoping it is already present...", e);
		}

		LOGGER.info("Removing context from index {}...", index);

		updateByQueryAsync(index, REMOVE_CONTEXT_SCRIPT_ID);
	}

	private void updateByQueryAsync(String index, String scriptId) {
		UpdateByQueryRequest update = new UpdateByQueryRequest(index);
		update.setScript(new Script(ScriptType.STORED, null, scriptId, Collections.emptyMap()));
		update.setTimeout(TimeValue.timeValueMinutes(5));

		client.updateByQueryAsync(update, RequestOptions.DEFAULT, new ActionListener<BulkByScrollResponse>() {

			@Override
			public void onResponse(BulkByScrollResponse response) {
				LOGGER.info("The update by query request to {} took {} and updated {} documents.", index, response.getTook(), response.getUpdated());
			}

			@Override
			public void onFailure(Exception e) {
				LOGGER.warn("The update by query request failed!", e);
			}
		});
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return new StringBuilder().append(aid.dropService()).append(".").append(tailoring).append(".intensity").toString();
	}

	@Override
	protected String serialize(IntensityRecord intensity) throws JsonProcessingException {
		return mapper.writeValueAsString(intensity);
	}

	@Override
	protected String getDocumentId(IntensityRecord intensity) {
		return Long.toString(intensity.getTimestamp());
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
