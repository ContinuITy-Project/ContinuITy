package org.continuity.cobra.managers;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionView;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
public class ElasticsearchSessionManager extends ElasticsearchScrollingManager<Session> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSessionManager.class);

	private final ObjectMapper mapper;

	public ElasticsearchSessionManager(String host, ObjectMapper mapper) throws IOException {
		super(host, "session");

		this.mapper = mapper;
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
	public void storeOrUpdateSessions(AppId aid, Collection<Session> sessions, List<String> tailoring) throws IOException {
		storeElements(aid, tailoring, sessions);
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
		QueryBuilder query = createRangeQuery(version, from, to);

		return readElements(aid, tailoring, query, String.format(" with version %s and time range %s - %s", version, formatOrNull(from), formatOrNull(to)));
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
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return 0;
		}

		CountRequest count = new CountRequest(index);
		count.source(new SearchSourceBuilder().query(createRangeQuery(version, from, to)));

		CountResponse response;
		try {
			response = client.count(count, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not find any sessions for app-id {} and version {} in time range {} - {}: {}", aid, version, formatOrNull(from), formatOrNull(to), e.getMessage());
			return 0;
		}

		return response.getCount();
	}

	private QueryBuilder createRangeQuery(VersionOrTimestamp version, Date from, Date to) {
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

		query.must(QueryBuilders.termQuery("tailoring", Session.convertTailoringToString(tailoring)));
		query.must(QueryBuilders.termQuery("finished", false));

		return readElements(aid, query, String.format("with version %s and open sessions", version));
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return new StringBuilder().append(aid.dropService()).append(".").append(tailoring).append(".sessions").toString();
	}

	@Override
	protected Pair<String, String> serialize(Session session) {
		try {
			return Pair.of(mapper.writerWithView(SessionView.Extended.class).writeValueAsString(session), session.getUniqueId());
		} catch (JsonProcessingException e) {
			LOGGER.error("Could not write TraceRecord to JSON string!", e);
			return null;
		}
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
