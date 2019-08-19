package org.continuity.cobra.managers;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.cobra.entities.TraceRecord;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
public class ElasticsearchTraceManager extends ElasticsearchScrollingManager<TraceRecord> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchTraceManager.class);

	private final ObjectMapper mapper;

	public ElasticsearchTraceManager(String host, ObjectMapper mapper) throws IOException {
		super(host, "trace");
		this.mapper = mapper;
	}

	/**
	 * Stores the passed traces to the elasticsearch database.
	 *
	 * @param aid
	 *            The app-id of the corresponding application.
	 * @param version
	 *            The version of the application.
	 * @param traces
	 *            The traces to be stored.
	 * @throws IOException
	 */
	public void storeTraces(AppId aid, VersionOrTimestamp version, List<Trace> traces) throws IOException {
		storeTraceRecords(aid, version, traces.stream().map(t -> new TraceRecord(version, t)).collect(Collectors.toList()));
	}

	/**
	 * Stores the passed trace records to the elasticsearch database.
	 *
	 * @param aid
	 *            The app-id of the corresponding application.
	 * @param version
	 *            The version of the application.
	 * @param traces
	 *            The trace records to be stored.
	 * @throws IOException
	 */
	public void storeTraceRecords(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces) throws IOException {
		storeElements(aid, traces);
	}

	/**
	 * Reads the traces of a given app-id, version (or timestamp), and time range from the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param from
	 *            The lower limit. {@code null} means unbound.
	 * @param to
	 *            The upper limit. {@code null} means unbound.
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 *             If a request to the database times out.
	 */
	public List<Trace> readTraces(AppId aid, VersionOrTimestamp version, Date from, Date to) throws IOException, TimeoutException {
		return readTraceRecords(aid, version, from, to).stream().map(TraceRecord::getTrace).collect(Collectors.toList());
	}

	/**
	 * Reads the traces of a given app-id, version (or timestamp), and time range from the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param from
	 *            The lower limit. {@code null} means unbound.
	 * @param to
	 *            The upper limit. {@code null} means unbound.
	 * @return The found traces as {@link TraceRecord}.
	 * @throws IOException
	 * @throws TimeoutException
	 *             If a request to the database times out.
	 */
	public List<TraceRecord> readTraceRecords(AppId aid, VersionOrTimestamp version, Date from, Date to) throws IOException, TimeoutException {
		QueryBuilder query = createRangeQuery(version, from, to);

		return readElements(aid, query, String.format("with version %s, and time range %s - %s", version, formatOrNull(from), formatOrNull(to)));
	}

	/**
	 * Counts all traces within a given range.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp. Can be {@code null}. In this case, it will be ignored.
	 * @param from
	 *            The start date of the range.
	 * @param to
	 *            The end date of the range.
	 * @return The number of sessions in the range.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public long countTraces(AppId aid, VersionOrTimestamp version, Date from, Date to) throws IOException {
		QueryBuilder query = createRangeQuery(version, from, to);

		return countElements(aid, query, String.format("with version %s, and time range %s - %s", version, formatOrNull(from), formatOrNull(to)));
	}

	private QueryBuilder createRangeQuery(VersionOrTimestamp version, Date from, Date to) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (version != null) {
			query = query.must(QueryBuilders.termQuery("version", version.toNormalizedString()));
		}

		if ((from != null) && (to != null)) {
			query.must(QueryBuilders.rangeQuery("trace.rootOfTrace.rootOfSubTrace.timeStamp").from(from.getTime(), false).to(to.getTime(), true));
		} else {
			LOGGER.warn("The provided time range ({} - {}) contains null elements! Ignoring.", from, to);
		}

		return query;
	}

	/**
	 * Reads all traces having one of the defined unique session IDs.
	 *
	 * @param aid
	 * @param rootEndpoint
	 *            The root endpoint to filter for. Can be {@code null}. In this case, it will be
	 *            ignored.
	 * @param uniqueSessionIds
	 *            The unique (!) session IDs.
	 * @return The found traces as {@link TraceRecord}.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public List<TraceRecord> readTraceRecords(AppId aid, String rootEndpoint, List<String> uniqueSessionIds) throws IOException, TimeoutException {
		BoolQueryBuilder query;

		TermsQueryBuilder sessionQuery = QueryBuilders.termsQuery("unique-session-ids", uniqueSessionIds);

		if (rootEndpoint != null) {
			query = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("endpoint", rootEndpoint)).must(sessionQuery);
		} else {
			query = QueryBuilders.boolQuery().must(sessionQuery);
		}

		return readElements(aid, query, "for unique IDs");
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return aid.dropService() + ".traces";
	}

	@Override
	protected String serialize(TraceRecord record) throws JsonProcessingException {
		return mapper.writeValueAsString(record);
	}

	@Override
	protected String getDocumentId(TraceRecord record) {
		return Long.toString(record.getTrace().getTraceId());
	}

	@Override
	protected TraceRecord deserialize(String json) {
		try {
			return mapper.readValue(json, TraceRecord.class);
		} catch (IOException e) {
			LOGGER.error("Could not read TraceRecord from JSON string!", e);
			return null;
		}
	}

}
