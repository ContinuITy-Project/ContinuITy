package org.continuity.cobra.amqp;

import java.io.IOException;
import java.util.Date;

import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.converter.ClustinatorMarkovChainConverter;
import org.continuity.cobra.entities.ClustinatorResult;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Receives and processes the results of the clustinator.
 *
 * @author Henning Schulz
 *
 */
@Component
public class ClustinatorResultAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClustinatorResultAmqpHandler.class);

	@Autowired
	private ElasticsearchBehaviorManager behaviorManager;

	@RabbitListener(queues = RabbitMqConfig.EVENT_CLUSTINATOR_FINISHED_QUEUE_NAME)
	public void processClustering(ClustinatorResult result) throws IOException {
		LOGGER.info("{}@{} {}: Received result from clustinator in range {} ({}) - {}", result.getAppId(), result.getVersion(), result.getTailoring(),
				new Date(result.getStartMicros() / 1000), new Date(result.getIntervalStartMicros() / 1000), new Date(result.getEndMicros() / 1000));

		storeBehaviorModel(result);
		updateIntensities(result);

		LOGGER.info("{}@{} {}: Processing of the clustinator result finished.", result.getAppId(), result.getVersion(), result.getTailoring());
	}

	private void storeBehaviorModel(ClustinatorResult result) throws IOException {
		ClustinatorMarkovChainConverter converter = new ClustinatorMarkovChainConverter(result.getStates());
		MarkovBehaviorModel behaviorModel = converter.convertArrays(result.getMeanMarkovChains());
		behaviorModel.setTimestamp(result.getIntervalStartMicros() / 1000);

		behaviorManager.store(result.getAppId(), result.getTailoring(), behaviorModel);
		LOGGER.info("{}@{} {}: Stored behavior model.", result.getAppId(), result.getVersion(), result.getTailoring());
	}

	private void updateIntensities(ClustinatorResult result) {
		// TODO: implement
		LOGGER.warn("{}@{} {}: Updating of the intensities is not implemented yet.", result.getAppId(), result.getVersion(), result.getTailoring());
	}

}
