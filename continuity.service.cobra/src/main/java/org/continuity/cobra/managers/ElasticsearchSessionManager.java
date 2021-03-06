package org.continuity.cobra.managers;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionView;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
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
public class ElasticsearchSessionManager extends ElasticsearchScrollingManager<Session> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSessionManager.class);

	private static final RequestOptions REQUEST_OPTIONS;

	// sessions can be larger
	private static final int SCROLL_SIZE = DEFAULT_SCROLL_SIZE / 4;

	private static final String UPDATE_SCRIPT_ID = "update-session";

	static {
		RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
		builder.setHttpAsyncResponseConsumerFactory(new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(Integer.MAX_VALUE));
		REQUEST_OPTIONS = builder.build();
	}

	private final ObjectMapper mapper;

	private boolean updateScriptInitialized = false;

	public ElasticsearchSessionManager(String host, ObjectMapper mapper, int bulkTimeoutSeconds) throws IOException {
		super(host, "session", bulkTimeoutSeconds, REQUEST_OPTIONS);

		this.mapper = mapper;
	}

	/**
	 * Stores the passed sessions for the given app-id. If there is already a session, the session
	 * fields will be overwritten and the requests will be added to the existing list.
	 *
	 * @param aid
	 *            The app-id.
	 * @param sessions
	 *            The sessions to be stored.
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	public void storeOrUpdateSessions(AppId aid, Collection<Session> sessions, List<String> tailoring, boolean waitFor) throws IOException {
		if (!updateScriptInitialized) {
			updateScriptInitialized = initUpdateScript(UPDATE_SCRIPT_ID);
		}

		storeOrUpdateByScript(aid, tailoring, sessions, this::createUpdateScript, true, waitFor);
	}

	private Script createUpdateScript(Session session) {
		@SuppressWarnings("unchecked")
		Map<String, Object> params = mapper.convertValue(session, HashMap.class);

		return new Script(ScriptType.STORED, null, UPDATE_SCRIPT_ID, params);
	}

	/**
	 * Reads all sessions overlapping a given range.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @param from
	 *            The start date of the range.
	 * @param to
	 *            The end date of the range.
	 * @return The list of sessions overlapping the range.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<Session> readSessionsOverlapping(AppId aid, VersionOrTimestamp version, List<String> tailoring, Date from, Date to) throws IOException, TimeoutException {
		QueryBuilder query = createOverlappingQuery(version, from, to);
		FieldSortBuilder sort = new FieldSortBuilder("start-micros").order(SortOrder.ASC);

		return readElements(aid, tailoring, query, sort, SCROLL_SIZE, TOTAL_SIZE_ALL,
				String.format(" with version %s and overlapping time range %s - %s", version, formatOrNull(from), formatOrNull(to)));
	}

	/**
	 * Reads all sessions starting in a given range.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @param from
	 *            The start date of the range.
	 * @param to
	 *            The end date of the range.
	 * @return The list of sessions starting in the range.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<Session> readSessionsStartingInRange(AppId aid, VersionOrTimestamp version, List<String> tailoring, Date from, Date to) throws IOException, TimeoutException {
		QueryBuilder query = createRangeStartQuery(version, from, to);
		FieldSortBuilder sort = new FieldSortBuilder("start-micros").order(SortOrder.ASC);

		return readElements(aid, tailoring, query, sort, SCROLL_SIZE, TOTAL_SIZE_ALL,
				String.format(" with version %s and starting in time range %s - %s", version, formatOrNull(from), formatOrNull(to)));
	}

	/**
	 * Counts all sessions that overlap a given range.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @param from
	 *            The start date of the range.
	 * @param to
	 *            The end date of the range.
	 * @return The number of sessions in the range.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public long countSessionsOverlapping(AppId aid, VersionOrTimestamp version, List<String> tailoring, Date from, Date to) throws IOException {
		QueryBuilder query = createRangeStartQuery(version, from, to);

		return countElements(aid, tailoring, query, String.format(" with version %s and time range %s - %s", version, formatOrNull(from), formatOrNull(to)));
	}

	/**
	 * Counts all sessions starting within a given range, grouped by the behavior group.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @param from
	 *            The start date of the range.
	 * @param to
	 *            The end date of the range.
	 * @param numGroups
	 *            The (maximum) number of groups to search for.
	 * @return A map of the group id to the number of sessions in the range.
	 * @throws IOException
	 */
	public Map<String, Long> countSessionStartsPerGroupInRange(AppId aid, VersionOrTimestamp version, List<String> tailoring, Date from, Date to, int numGroups) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return Collections.emptyMap();
		}

		SearchSourceBuilder source = new SearchSourceBuilder();

		source.query(createRangeStartQuery(version, from, to));
		source.size(0);
		source.aggregation(AggregationBuilders.terms("num_sessions").field("group-id").size(numGroups));

		SearchRequest search = new SearchRequest(index).source(source);

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", aid, index, e.getMessage());
			return Collections.emptyMap();
		}

		Terms numSessions = response.getAggregations().get("num_sessions");
		Map<String, Long> sessionsPerGroup = new HashMap<>();

		for (Bucket bucket : numSessions.getBuckets()) {
			sessionsPerGroup.put(bucket.getKeyAsString(), bucket.getDocCount());
		}

		return sessionsPerGroup;
	}

	private QueryBuilder createRangeStartQuery(VersionOrTimestamp version, Date from, Date to) {
		boolean empty = true;

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
			empty = false;
		}

		if ((from != null) && (to != null)) {
			query.must(QueryBuilders.rangeQuery("start-micros").from(from.getTime() * 1000, false).to(to.getTime() * 1000, true));
			empty = false;
		} else {
			LOGGER.warn("The provided time range ({} - {}) contains null elements! Ignoring.", formatOrNull(from), formatOrNull(to));
		}

		if (empty) {
			return QueryBuilders.matchAllQuery();
		} else {
			return query;
		}
	}

	private QueryBuilder createOverlappingQuery(VersionOrTimestamp version, Date from, Date to) {
		boolean empty = true;

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
			empty = false;
		}

		if (from != null) {
			query.must(QueryBuilders.rangeQuery("end-micros").from(from.getTime() * 1000, true));
			empty = false;
		}

		if (to != null) {
			query.must(QueryBuilders.rangeQuery("start-micros").to(to.getTime() * 1000, true));
			empty = false;
		}

		if (empty) {
			LOGGER.warn("The provided time range is null! Ignoring.");
		}

		if (empty) {
			return QueryBuilders.matchAllQuery();
		} else {
			return query;
		}
	}

	/**
	 * Reads all sessions of a given app-id that are not finished, yet.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @return The list of open sessions.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	public List<Session> readOpenSessions(AppId aid, VersionOrTimestamp version, List<String> tailoring) throws IOException, TimeoutException {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
		}

		query.must(QueryBuilders.termQuery("finished", false));

		return readElementsExcluding(aid, query, SCROLL_SIZE, TOTAL_SIZE_ALL, String.format("with version %s and open sessions", version), "requests.*");
	}

	/**
	 * Reads all sessions having one of the passed IDs.
	 *
	 * @param aid
	 *            The app-id.
	 * @param sessionIds
	 *            The IDs of the searched sessions.
	 * @return The list of found sessions.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<Session> readSessionsWithId(AppId aid, List<String> sessionIds) throws IOException, TimeoutException {
		return readSessionsWithId(aid, sessionIds.toArray(new String[sessionIds.size()]));
	}

	/**
	 * Reads all sessions having one of the passed IDs.
	 *
	 * @param aid
	 *            The app-id.
	 * @param sessionIds
	 *            The IDs of the searched sessions.
	 * @return The list of found sessions.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<Session> readSessionsWithId(AppId aid, String... sessionIds) throws IOException, TimeoutException {
		IdsQueryBuilder query = new IdsQueryBuilder().addIds(sessionIds);
		List<Session> sessions = readElements(aid, null, query, null, SCROLL_SIZE, TOTAL_SIZE_ALL, String.format("with one of the %d passed IDs", sessionIds.length));

		if (sessions.size() != sessionIds.length) {
			LOGGER.warn("Found {} sessions for app-id {}, but searched for {} session IDs!", sessions.size(), aid, sessionIds.length);
		}

		return sessions;
	}

	/**
	 * Scrolls for the sessions with a given group-id in a given time range and calls a callback for
	 * each retrieved chunk of sessions.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 * @param groupId
	 *            The group-id.
	 * @param fromMicros
	 *            The lower limit of the time range. The <b>end</b>-micros of the sessions need to
	 *            be after this time stamp.
	 * @param toMicros
	 *            The upper limit of the time range. The <b>start</b>-micros of the sessions need to
	 *            be before this time stamp.
	 * @param callback
	 *            Will be called for each retrieved chunk.
	 * @param includeRequests
	 *            if {@code false}, the returned sessions won't contain the requests.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public void scrollForSessionsWithGroupId(AppId aid, List<String> tailoring, String groupId, long fromMicros, long toMicros, Consumer<List<Session>> callback, boolean includeRequests)
			throws IOException, TimeoutException {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		query.must(QueryBuilders.termQuery("group-id", groupId));

		query.must(QueryBuilders.rangeQuery("start-micros").to(toMicros, true));
		query.must(QueryBuilders.rangeQuery("end-micros").from(fromMicros, true));

		FieldSortBuilder sort = new FieldSortBuilder("start-micros").order(SortOrder.ASC);

		String[] excludes = includeRequests ? null : new String[] { "requests" };

		scrollForElements(aid, tailoring, query, sort, SCROLL_SIZE, TOTAL_SIZE_ALL,
				String.format("with group-id %s in range %s - %s", groupId, formatOrNull(new Date(fromMicros / 1000)), formatOrNull(new Date(toMicros / 1000))), null, excludes, callback);
	}

	/**
	 * Gets the latest date occurring in the stored sessions.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @return The found date. In case no sessions could be found, 1970-01-01 01:00:00 will be
	 *         returned.
	 * @throws IOException
	 */
	public Date getLatestDate(AppId aid, VersionOrTimestamp version, List<String> tailoring) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return new Date(0);
		}

		SearchSourceBuilder source = new SearchSourceBuilder();

		if (version != null) {
			source.query(QueryBuilders.termQuery("version", Session.convertTailoringToString(tailoring)));
		}

		source.aggregation(AggregationBuilders.max("max_timestamp").field("end-micros").missing(0));

		SearchRequest search = new SearchRequest(index).source(source);

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", aid, index, e.getMessage());
			return new Date(0);
		}

		ParsedMax max = response.getAggregations().get("max_timestamp");
		double micros = max.getValue();

		return new Date(Math.round(micros / 1000));
	}

	/**
	 * Gets the earliest date occurring in the stored sessions.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param tailoring
	 *            The list of services to which the sessions are tailored. Use a singleton list with
	 *            {@link AppId#SERVICE_ALL} to get untailored sessions.
	 * @return The found date. In case no sessions could be found, 292278994-08-17 08:12:55 (max
	 *         value of long) will be returned.
	 * @throws IOException
	 */
	public Date getEarliestDate(AppId aid, VersionOrTimestamp version, List<String> tailoring) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return new Date(Long.MAX_VALUE);
		}

		SearchSourceBuilder source = new SearchSourceBuilder();

		if (version != null) {
			source.query(QueryBuilders.termQuery("version", Session.convertTailoringToString(tailoring)));
		}

		source.aggregation(AggregationBuilders.min("min_timestamp").field("start-micros").missing(0));

		SearchRequest search = new SearchRequest(index).source(source);

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", aid, index, e.getMessage());
			return new Date(Long.MAX_VALUE);
		}

		ParsedMin min = response.getAggregations().get("min_timestamp");
		double micros = min.getValue();

		return new Date(Math.round(micros / 1000));
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return new StringBuilder().append(aid.dropService()).append(".").append(tailoring).append(".sessions").toString();
	}

	@Override
	protected String serialize(Session session) throws JsonProcessingException {
		return mapper.writerWithView(SessionView.Internal.class).writeValueAsString(session);
	}

	@Override
	protected String getDocumentId(Session session) {
		return session.getUniqueId();
	}

	@Override
	protected Session deserialize(String json) {
		try {
			return mapper.readValue(json, Session.class);
		} catch (IOException e) {
			LOGGER.error("Could not read Session from JSON string!", e);
			return null;
		}
	}

}
