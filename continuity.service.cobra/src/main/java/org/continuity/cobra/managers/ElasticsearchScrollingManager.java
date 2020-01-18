package org.continuity.cobra.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.idpa.AppId;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class ElasticsearchScrollingManager<T> {

	protected static final int DEFAULT_SCROLL_SIZE = 10000; // is the maximum

	protected static final int TOTAL_SIZE_ALL = -1;

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchScrollingManager.class);

	private static final long SCROLL_MINUTES = 1;

	protected final RestHighLevelClient client;

	private final String mapping;

	private final long bulkTimeoutSeconds;

	protected ElasticsearchScrollingManager(String host, String mappingName, int bulkTimeoutSeconds) throws IOException {
		this.client = new RestHighLevelClient(
				RestClient.builder(new HttpHost(host, 9200, "http"), new HttpHost(host, 9300, "http")).setRequestConfigCallback(cb -> cb.setSocketTimeout(bulkTimeoutSeconds * 1000)));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + mappingName + "-mapping.json")))) {
			this.mapping = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}

		this.bulkTimeoutSeconds = bulkTimeoutSeconds;
	}

	public void destroy() throws IOException {
		client.close();
	}

	protected abstract String toIndex(AppId aid, String tailoring);

	/**
	 * Transforms an element to JSON.
	 *
	 * @param element
	 * @return the JSON.
	 * @throws JsonProcessingException
	 *             if the JSON serialization fails.
	 */
	protected abstract String serialize(T element) throws JsonProcessingException;

	protected abstract String getDocumentId(T element);

	protected abstract T deserialize(String json);

	/**
	 * Stores the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param elements
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	protected void storeElements(AppId aid, Collection<T> elements, boolean waitFor) throws IOException {
		storeElements(aid, Collections.emptyList(), elements, waitFor);
	}

	/**
	 * Stores the elements using the default tailoring (all) without waiting for the data being
	 * indexed.
	 *
	 * @param aid
	 * @param elements
	 * @throws IOException
	 */
	protected void storeElements(AppId aid, Collection<T> elements) throws IOException {
		storeElements(aid, elements, false);
	}

	/**
	 * Stores the elements using the defined tailoring.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	protected void storeElements(AppId aid, List<String> tailoring, Collection<T> elements, boolean waitFor) throws IOException {
		storeElements(aid, tailoring, elements, waitFor, false, false);
	}

	/**
	 * Stores the elements if absent using the defined tailoring.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	protected void storeElementsIfAbsent(AppId aid, List<String> tailoring, Collection<T> elements, boolean waitFor) throws IOException {
		storeElements(aid, tailoring, elements, waitFor, true, true);
	}

	private void storeElements(AppId aid, List<String> tailoring, Collection<T> elements, boolean waitFor, boolean create, boolean silent) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));
		initIndex(index);

		doBulkRequest(index, elements, waitFor, true, (request, element, json, id) -> request.add(new IndexRequest(index).source(json, XContentType.JSON).id(id).create(create)), silent);
	}

	/**
	 * Stores the elements or updates existing ones. The already stored fields of elements that are
	 * not contained in the new JSON will be retained.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @throws IOException
	 */
	protected void storeOrUpdateElements(AppId aid, List<String> tailoring, Collection<T> elements) throws IOException {
		storeOrUpdateElements(aid, tailoring, elements, false);
	}

	/**
	 * Stores the elements or updates existing ones. The already stored fields of elements that are
	 * not contained in the new JSON will be retained.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	protected void storeOrUpdateElements(AppId aid, List<String> tailoring, Collection<T> elements, boolean waitFor) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));
		initIndex(index);

		doBulkRequest(index, elements, waitFor, true, (request, element, json, id) -> request.add(new UpdateRequest(index, id).doc(json, XContentType.JSON).docAsUpsert(true)), false);
	}

	/**
	 * Updates the elements by using the provided scripts or stores the respective element if there
	 * is none, yet.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @param scriptSupplier
	 * @throws IOException
	 */
	protected void storeOrUpdateByScript(AppId aid, List<String> tailoring, Collection<T> elements, Function<T, Script> scriptSupplier) throws IOException {
		storeOrUpdateByScript(aid, tailoring, elements, scriptSupplier, false);
	}

	/**
	 * Updates the elements by using the provided scripts or stores the respective element if there
	 * is none, yet.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @param scriptSupplier
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	protected void storeOrUpdateByScript(AppId aid, List<String> tailoring, Collection<T> elements, Function<T, Script> scriptSupplier, boolean waitFor) throws IOException {
		storeOrUpdateByScript(aid, tailoring, elements, scriptSupplier, false, waitFor);
	}

	/**
	 * Updates the elements by using the provided scripts or stores the respective element if there
	 * is none, yet.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @param scriptSupplier
	 * @param scriptedUpsert
	 *            Whether the script should also be executed if the document does not exist yet.
	 * @param waitFor
	 *            Whether the request should wait until the data is indexed.
	 * @throws IOException
	 */
	protected void storeOrUpdateByScript(AppId aid, List<String> tailoring, Collection<T> elements, Function<T, Script> scriptSupplier, boolean scriptedUpsert, boolean waitFor) throws IOException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));
		initIndex(index);

		doBulkRequest(index, elements, waitFor, !scriptedUpsert, (request, element, json, id) -> {
			UpdateRequest update = new UpdateRequest(index, id).script(scriptSupplier.apply(element));

			if (scriptedUpsert) {
				update.scriptedUpsert(true).upsert();
			} else {
				update.upsert(json, XContentType.JSON);
			}

			request.add(update);
		}, false);
	}

	private void doBulkRequest(String index, Collection<T> elements, boolean waitFor, boolean requiresJson, RequestAdder<T> requestAdder, boolean silent) throws IOException {
		BulkRequest request = new BulkRequest();

		for (T elem : elements) {
			requestAdder.add(request, elem, requiresJson ? serialize(elem) : null, getDocumentId(elem));
		}

		if (waitFor) {
			request.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
		}

		request.timeout(TimeValue.timeValueSeconds(bulkTimeoutSeconds));

		BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);

		LOGGER.info("The bulk request to {} took {} and resulted in status {}.", index, response.getTook(), response.status());

		if (!silent) {
			List<T> failedElements = filterFailed(elements, response);

			if (failedElements.size() > 0) {
				LOGGER.warn("{} of the {} bulk items failed! Retrying...", failedElements.size(), elements.size());

				doBulkRequest(index, failedElements, waitFor, requiresJson, requestAdder, silent);
			}
		}
	}

	private List<T> filterFailed(Collection<T> elements, BulkResponse response) {
		List<T> filteredElements = new ArrayList<>();

		Iterator<T> elemIter = elements.iterator();
		Iterator<BulkItemResponse> itemIter = Arrays.stream(response.getItems()).iterator();

		while (elemIter.hasNext() && itemIter.hasNext()) {
			T elem = elemIter.next();

			if (itemIter.next().isFailed()) {
				filteredElements.add(elem);
			}
		}

		return filteredElements;
	}

	/**
	 * Stores the elements using the defined tailoring without waiting for the data being indexed.
	 *
	 * @param aid
	 * @param tailoring
	 * @param elements
	 * @throws IOException
	 */
	protected void storeElements(AppId aid, List<String> tailoring, Collection<T> elements) throws IOException {
		storeElements(aid, tailoring, elements, false);
	}

	/**
	 * Reads the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param query
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElements(AppId aid, QueryBuilder query, String message) throws IOException, TimeoutException {
		return readElements(aid, Collections.emptyList(), query, message);
	}

	/**
	 * Reads the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param query
	 * @param scrollSize
	 * @param totalSize
	 *            -1 means infinite
	 * @param message
	 * @param fields
	 *            The object fields to include in the response.
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElementsIncluding(AppId aid, QueryBuilder query, int scrollSize, int totalSize, String message, String... fields) throws IOException, TimeoutException {
		return readElements(aid, Collections.emptyList(), query, null, scrollSize, totalSize, message, fields, null);
	}

	/**
	 * Reads the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param query
	 * @param scrollSize
	 * @param totalSize
	 *            -1 means infinite
	 * @param message
	 * @param fields
	 *            The object fields to exclude in the response.
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElementsExcluding(AppId aid, QueryBuilder query, int scrollSize, int totalSize, String message, String... fields) throws IOException, TimeoutException {
		return readElements(aid, Collections.emptyList(), query, null, scrollSize, totalSize, message, null, fields);
	}

	/**
	 * Reads the elements using the defined tailoring.
	 *
	 * @param aid
	 * @param tailoring
	 * @param query
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElements(AppId aid, List<String> tailoring, QueryBuilder query, String message) throws IOException, TimeoutException {
		return readElements(aid, tailoring, query, null, DEFAULT_SCROLL_SIZE, TOTAL_SIZE_ALL, message, null, null);
	}

	/**
	 * Reads the elements using the defined tailoring, sort, and number of elements to be retrieved.
	 *
	 * @param aid
	 * @param tailoring
	 * @param query
	 * @param sort
	 * @param scrollSize
	 * @param totalSize
	 *            -1 means infinite
	 * @param message
	 * @param includes
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElements(AppId aid, List<String> tailoring, QueryBuilder query, SortBuilder<?> sort, int scrollSize, int totalSize, String message) throws IOException, TimeoutException {
		return readElements(aid, tailoring, query, sort, scrollSize, totalSize, message, null, null);
	}

	/**
	 * Reads the elements using the defined tailoring, sort, and number of elements to be retrieved.
	 *
	 * @param aid
	 * @param tailoring
	 * @param query
	 * @param sort
	 * @param scrollSize
	 * @param totalSize
	 *            -1 means infinite
	 * @param message
	 * @param includes
	 *            The object fields to include in the response.
	 * @param excludes
	 *            The object fields to exclude in the response.
	 * @return
	 * @throws IOException
	 * @throws TimeoutException
	 */
	protected List<T> readElements(AppId aid, List<String> tailoring, QueryBuilder query, SortBuilder<?> sort, int scrollSize, int totalSize, String message, String[] includes, String[] excludes)
			throws IOException, TimeoutException {
		List<T> result = new ArrayList<>();

		scrollForElements(aid, tailoring, query, sort, scrollSize, totalSize, message, includes, excludes, result::addAll);

		return result;
	}

	protected void scrollForElements(AppId aid, List<String> tailoring, QueryBuilder query, SortBuilder<?> sort, int scrollSize, int totalSize, String message, String[] includes, String[] excludes,
			Consumer<List<T>> callback) throws IOException, TimeoutException {
		String index = toIndex(aid, Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return;
		}

		SearchSourceBuilder source = new SearchSourceBuilder().query(query).size(scrollSize);

		if (((includes != null) && (includes.length > 0)) || ((excludes != null) && (excludes.length > 0))) {
			source.fetchSource(includes, excludes);
		}

		if (sort != null) {
			source.sort(sort);
		}

		SearchRequest search = new SearchRequest(index).source(source).scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));

		SearchResponse response;
		try {
			response = client.search(search, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not get any elements from {} {}: {}", index, message, e.getMessage());
			return;
		}

		SearchHits hits = response.getHits();
		LOGGER.info("The search request to {} {} resulted in {}.", index, message, hits.getTotalHits());

		processSearchResponse(response, aid, message, 0, totalSize, callback);
	}

	private void processSearchResponse(SearchResponse response, AppId aid, String message, int scrollNumber, int remaining, Consumer<List<T>> callback) throws IOException, TimeoutException {
		if (response.isTimedOut()) {
			throw new TimeoutException(String.format("The search request to app-id %s and version %s timed out!", aid.toString()));
		}

		SearchHits hits = response.getHits();
		String scrollId = response.getScrollId();
		int numHits = hits.getHits().length;

		LOGGER.info("Scroll #{} took {}, had {} hits, and is {}.", scrollNumber, response.getTook(), numHits, response.status());

		callback.accept(Arrays.stream(hits.getHits()).map(SearchHit::getSourceAsString).map(this::deserialize).filter(Objects::nonNull).collect(Collectors.toList()));

		if ((numHits > 0) && ((remaining < 0) || (numHits < remaining))) {
			scrollForMoreElements(scrollId, aid, message, scrollNumber, remaining - numHits, callback);
		} else {
			LOGGER.info("Reached end of scroll.");
			clearScroll(scrollId);
		}
	}

	private void scrollForMoreElements(String scrollId, AppId aid, String message, int scrollNumber, int remaining, Consumer<List<T>> callback) throws IOException, TimeoutException {
		SearchScrollRequest scroll = new SearchScrollRequest(scrollId);
		scroll.scroll(TimeValue.timeValueMinutes(SCROLL_MINUTES));
		processSearchResponse(client.scroll(scroll, RequestOptions.DEFAULT), aid, message, scrollNumber + 1, remaining, callback);
	}

	private void clearScroll(String scrollId) throws IOException {
		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(scrollId);
		ClearScrollResponse response = client.clearScroll(request, RequestOptions.DEFAULT);

		if (response.isSucceeded()) {
			LOGGER.info("Cleared the scroll with ID {}. Freed {} contexts.", scrollId, response.getNumFreed());
		} else {
			LOGGER.error("Could not clear the scroll with ID {}: {}", scrollId, response.status());
		}
	}

	/**
	 * Counts the elements using the default tailoring (all).
	 *
	 * @param aid
	 * @param query
	 * @param message
	 * @return
	 * @throws IOException
	 */
	protected long countElements(AppId aid, QueryBuilder query, String message) throws IOException {
		return countElements(aid, Collections.emptyList(), query, message);
	}

	/**
	 * Counts the elements using the defined tailoring.
	 *
	 * @param aid
	 * @param tailoring
	 * @param query
	 * @param message
	 * @return
	 * @throws IOException
	 */
	protected long countElements(AppId aid, List<String> tailoring, QueryBuilder query, String message) throws IOException {
		String index = toIndex(aid, tailoring == null ? null : Session.convertTailoringToString(tailoring));

		if (!indexExists(index)) {
			return 0;
		}

		CountRequest count = new CountRequest(index);
		count.source(new SearchSourceBuilder().query(query));

		CountResponse response;
		try {
			response = client.count(count, RequestOptions.DEFAULT);
		} catch (ElasticsearchStatusException e) {
			LOGGER.info("Could not find any elements in {} {}: {}", index, message, e.getMessage());
			return 0;
		}

		return response.getCount();
	}

	/**
	 * Checks whether a certain index exists.
	 *
	 * @param index
	 *            The index to check.
	 * @return {@code true} if it exists.
	 * @throws IOException
	 */
	protected boolean indexExists(String index) throws IOException {
		GetIndexRequest request = new GetIndexRequest(index);
		return client.indices().exists(request, RequestOptions.DEFAULT);
	}

	protected void initIndex(String index) throws IOException {
		if (indexExists(index)) {
			return;
		}

		CreateIndexRequest request = new CreateIndexRequest(index);
		request.mapping(mapping, XContentType.JSON);
		CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

		if (response.isAcknowledged()) {
			LOGGER.info("Index {} has been created.", index);
		} else {
			LOGGER.warn("Creation of index {} has not been acknowledged!", index);
		}
	}

	protected boolean initUpdateScript(String scriptId) throws IOException {
		String scriptSource;

		try (InputStream in = ElasticsearchScrollingManager.class.getResourceAsStream("/" + scriptId + ".painless")) {
			scriptSource = new BufferedReader(new InputStreamReader(in)).lines().map(String::trim).collect(Collectors.joining(" "));
		}

		XContentBuilder script = XContentFactory.jsonBuilder();
		script.startObject();
		{
			script.startObject("script");
			{
				script.field("lang", "painless");
				script.field("source", scriptSource);
			}
			script.endObject();
		}
		script.endObject();

		PutStoredScriptRequest request = new PutStoredScriptRequest().id(scriptId).content(BytesReference.bytes(script), XContentType.JSON);

		AcknowledgedResponse response = client.putScript(request, RequestOptions.DEFAULT);

		if (response.isAcknowledged()) {
			LOGGER.info("Initialized update script.");
			return true;
		} else {
			LOGGER.error("Could not initialize update script! Elasticsearch did not acknowledge the request.");
			return false;
		}
	}

	protected String formatOrNull(Date date) {
		if (date == null) {
			return null;
		} else {
			return ApiFormats.DATE_FORMAT.format(date);
		}
	}

	private interface RequestAdder<T> {

		void add(BulkRequest request, T element, String json, String id);

	}

}
