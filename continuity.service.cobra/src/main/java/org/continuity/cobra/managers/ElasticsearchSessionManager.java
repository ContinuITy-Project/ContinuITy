package org.continuity.cobra.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionView;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
public class ElasticsearchSessionManager extends ElasticsearchScrollingManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSessionManager.class);

	private static final long SCROLL_MINUTES = 1;

	private final ObjectMapper mapper;

	public ElasticsearchSessionManager(String host, ObjectMapper mapper) throws IOException {
		super(host, "session");

		this.mapper = mapper;
	}

	public void destroy() throws IOException {
		client.close();
	}

	/**
	 * Stores the passed sessions for the given app-id, potentially overwriting old versions of the
	 * sessions.
	 *
	 * @param aid
	 *            The app-id.
	 * @param sessions
	 *            The sessions to be stored.
	 * @throws IOException
	 */
	public void storeOrUpdateSessions(AppId aid, Collection<Session> sessions) throws IOException {
		initIndex(toSessionIndex(aid));

		BulkRequest request = new BulkRequest();

		sessions.stream().map(this::serializeSession).filter(Objects::nonNull).forEach(json -> {
			request.add(new IndexRequest(toSessionIndex(aid)).source(json.getLeft(), XContentType.JSON).id(json.getRight()));
		});

		BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);

		LOGGER.info("The bulk request to app-id {} took {} and resulted in status {}.", aid, response.getTook(), response.status());
	}

	private Pair<String, String> serializeSession(Session session) {
		try {
			return Pair.of(mapper.writerWithView(SessionView.Extended.class).writeValueAsString(session), session.getUniqueId());
		} catch (JsonProcessingException e) {
			LOGGER.error("Could not write TraceRecord to JSON string!", e);
			return null;
		}
	}

	/**
	 * Reads all sessions within a given range.
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
	 * @return The list of sessions in the range.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<Session> readSessionsInRange(AppId aid, VersionOrTimestamp version, List<String> tailoring, Date from, Date to) throws IOException, TimeoutException {
		if (!indexExists(toSessionIndex(aid))) {
			return Collections.emptyList();
		}

		SearchRequest search = new SearchRequest(toSessionIndex(aid));
		search.source(createRangeSearch(version, tailoring, from, to).size(10000)); // This is the
																					// maximum

		search.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any sessions for app-id {} and version {} in time range {} - {}: {}", aid, version, formatOrNull(from), formatOrNull(to), e.getMessage());
			return Collections.emptyList();
		}

		SearchHits hits = response.getHits();
		LOGGER.info("The search request to app-id {}, version {}, and time range {} - {} resulted in {}.", aid, version, formatOrNull(from), formatOrNull(to), hits.getTotalHits());

		return processSearchResponse(response, aid, version, 0);
	}

	/**
	 * Counts all sessions within a given range.
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
	public long countSessionsInRange(AppId aid, VersionOrTimestamp version, List<String> tailoring, Date from, Date to) throws IOException {
		if (!indexExists(toSessionIndex(aid))) {
			return 0;
		}

		CountRequest count = new CountRequest(toSessionIndex(aid));
		count.source(createRangeSearch(version, tailoring, from, to));

		CountResponse response;
		try {
			response = client.count(count, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not find any sessions for app-id {} and version {} in time range {} - {}: {}", aid, version, formatOrNull(from), formatOrNull(to), e.getMessage());
			return 0;
		}

		return response.getCount();
	}

	private SearchSourceBuilder createRangeSearch(VersionOrTimestamp version, List<String> tailoring, Date from, Date to) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
		}

		query.must(QueryBuilders.termQuery("tailoring", Session.convertTailoringToString(tailoring)));

		if ((from != null) && (to != null)) {
			query.must(QueryBuilders.rangeQuery("start-micros").from(from.getTime() * 1000, false).to(to.getTime() * 1000, true));
		} else {
			LOGGER.warn("The provided time range ({} - {}) contains null elements! Ignoring.", from, to);
		}

		return new SearchSourceBuilder().query(query);
	}

	private String formatOrNull(Date date) {
		if (date == null) {
			return null;
		} else {
			return ApiFormats.DATE_FORMAT.format(date);
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
		if (!indexExists(toSessionIndex(aid))) {
			return Collections.emptyList();
		}

		SearchRequest search = new SearchRequest(toSessionIndex(aid));

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
		}

		query.must(QueryBuilders.termQuery("tailoring", Session.convertTailoringToString(tailoring)));
		query.must(QueryBuilders.termQuery("finished", false));
		search.source(new SearchSourceBuilder().query(query).size(10000)); // This is the maximum

		search.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any sessions for app-id {} and version {}: {}", aid, version, e.getMessage());
			return Collections.emptyList();
		}

		SearchHits hits = response.getHits();
		LOGGER.info("The search request to app-id {}, version {}, and open sessions resulted in {}.", aid, version, hits.getTotalHits());

		return processSearchResponse(response, aid, version, 0);
	}

	private List<Session> processSearchResponse(SearchResponse response, AppId aid, VersionOrTimestamp version, int scrollNumber) throws IOException, TimeoutException {
		if (response.isTimedOut()) {
			throw new TimeoutException(String.format("The search request to app-id %s and version %s timed out!", aid.toString(), version.toString()));
		}

		SearchHits hits = response.getHits();
		String scrollId = response.getScrollId();

		LOGGER.info("Scroll #{} took {} and is {}.", scrollNumber, response.getTook(), response.status());

		if (hits.getHits().length > 0) {
			List<Session> sessions = new ArrayList<>();
			Arrays.stream(hits.getHits()).map(SearchHit::getSourceAsString).map(this::readFromString).filter(Objects::nonNull).forEach(sessions::add);

			sessions.addAll(scrollForSessions(scrollId, aid, version, scrollNumber));

			return sessions;
		} else {
			LOGGER.info("Reached end of scroll.");
			clearScroll(scrollId);
			return Collections.emptyList();
		}
	}

	private List<Session> scrollForSessions(String scrollId, AppId aid, VersionOrTimestamp version, int scrollNumber) throws IOException, TimeoutException {
		SearchScrollRequest scroll = new SearchScrollRequest(scrollId);
		scroll.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));
		return processSearchResponse(client.scroll(scroll, RequestOptions.DEFAULT), aid, version, scrollNumber + 1);
	}

	private Session readFromString(String json) {
		try {
			return mapper.readValue(json, Session.class);
		} catch (IOException e) {
			LOGGER.error("Could not read Session from JSON string!", e);
			return null;
		}
	}

	private String toSessionIndex(AppId aid) {
		return aid.dropService() + ".sessions";
	}

}
