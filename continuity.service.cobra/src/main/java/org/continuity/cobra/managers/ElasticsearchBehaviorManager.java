package org.continuity.cobra.managers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
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

	public ElasticsearchBehaviorManager(String host, ObjectMapper mapper) throws IOException {
		super(host, "behavior");

		this.mapper = mapper;
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

		List<MarkovBehaviorModel> models = readElements(aid, tailoring, QueryBuilders.matchAllQuery(), sort, 1, "for latest behavior model");
		return ((models == null) || (models.size() == 0)) ? null : models.get(0);
	}

	@Override
	protected String toIndex(AppId aid, String tailoring) {
		return new StringBuilder().append(aid.dropService()).append(".").append(tailoring).append(".behavior").toString();
	}

	@Override
	protected Pair<String, String> serialize(MarkovBehaviorModel model) {
		try {
			return Pair.of(mapper.writeValueAsString(model), Long.toString(model.getTimestamp()));
		} catch (JsonProcessingException e) {
			LOGGER.error("Could not write MarkovBehaviorModel to JSON string!", e);
			return null;
		}
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
