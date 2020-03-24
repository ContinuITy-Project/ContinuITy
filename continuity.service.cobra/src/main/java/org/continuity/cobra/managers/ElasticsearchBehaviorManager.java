package org.continuity.cobra.managers;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.idpa.AppId;
import org.elasticsearch.index.query.QueryBuilders;
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
public class ElasticsearchBehaviorManager extends ElasticsearchScrollingManager<MarkovBehaviorModel> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchBehaviorManager.class);

	private final ObjectMapper mapper;

	public ElasticsearchBehaviorManager(String host, ObjectMapper mapper, int bulkTimeoutSeconds) throws IOException {
		super(host, "behavior", bulkTimeoutSeconds);

		this.mapper = mapper;
	}

	/**
	 * Stores a behavior model.
	 *
	 * @param aid
	 * @param tailoring
	 * @param model
	 *            The behavior model.
	 * @param waitFor
	 *            Whether to wait until the model storing is completed.
	 * @throws IOException
	 */
	public void store(AppId aid, List<String> tailoring, MarkovBehaviorModel model, boolean waitFor) throws IOException {
		storeElements(aid, tailoring, Collections.singleton(model), waitFor);
	}

	/**
	 * Reads the latest behavior model, i.e., the one with the greatest timestamp.
	 *
	 * @param aid
	 * @param tailoring
	 * @return The latest model or {@code null} if there is none.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public MarkovBehaviorModel readLatest(AppId aid, List<String> tailoring) throws IOException, TimeoutException {
		FieldSortBuilder sort = new FieldSortBuilder("timestamp").order(SortOrder.DESC);

		List<MarkovBehaviorModel> models = readElements(aid, tailoring, QueryBuilders.matchAllQuery(), sort, 1, 1, "for latest behavior model");
		return ((models == null) || (models.size() == 0)) ? null : models.get(0);
	}

	/**
	 * Reads the latest behavior model, i.e., the one with the greatest timestamp (before a given
	 * timestamp).
	 *
	 * @param aid
	 * @param tailoring
	 * @param before
	 *            A timestamp before which the result must be.
	 * @return The latest model or {@code null} if there is none.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	public MarkovBehaviorModel readLatest(AppId aid, List<String> tailoring, long before) throws IOException, TimeoutException {
		FieldSortBuilder sort = new FieldSortBuilder("timestamp").order(SortOrder.DESC);

		List<MarkovBehaviorModel> models = readElements(aid, tailoring, QueryBuilders.rangeQuery("timestamp").lte(before), sort, 1, 1, "for latest behavior model before " + new Date(before));
		return ((models == null) || (models.size() == 0)) ? null : models.get(0);
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return new StringBuilder().append(aid.dropService()).append(".").append(tailoring).append(".behavior").toString();
	}

	@Override
	protected String serialize(MarkovBehaviorModel model) throws JsonProcessingException {
		return mapper.writeValueAsString(model);
	}

	@Override
	protected String getDocumentId(MarkovBehaviorModel model) {
		return Long.toString(model.getTimestamp());
	}

	@Override
	protected MarkovBehaviorModel deserialize(String json) {
		try {
			return mapper.readValue(json, MarkovBehaviorModel.class);
		} catch (IOException e) {
			LOGGER.error("Could not read MarkovBehaviorModel from JSON string!", e);
			return null;
		}
	}

}
