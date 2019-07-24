package org.continuity.cobra.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class ElasticsearchScrollingManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchScrollingManager.class);

	protected final RestHighLevelClient client;

	private final String mapping;

	protected ElasticsearchScrollingManager(String host, String mappingName) throws IOException {
		this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, 9200, "http"), new HttpHost(host, 9300, "http")));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + mappingName + "-mapping.json")))) {
			this.mapping = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	protected void clearScroll(String scrollId) throws IOException {
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

}
