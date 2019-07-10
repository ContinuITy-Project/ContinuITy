package org.continuity.session.logs.managers;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
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

	public ElasticsearchScrollingManager(String host) {
		this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, 9200, "http"), new HttpHost(host, 9300, "http")));
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

}
