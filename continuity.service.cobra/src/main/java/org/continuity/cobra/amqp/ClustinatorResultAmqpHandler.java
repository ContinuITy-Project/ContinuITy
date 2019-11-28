package org.continuity.cobra.amqp;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.converter.ClustinatorMarkovChainConverter;
import org.continuity.cobra.entities.ClustinatorResult;
import org.continuity.cobra.entities.TraceProcessingStatus;
import org.continuity.cobra.extractor.IntensityCalculator;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

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
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@Autowired
	private ElasticsearchBehaviorManager behaviorManager;

	@Autowired
	private ElasticsearchIntensityManager intensityManager;

	@Autowired
	private TraceProcessingStatus status;

	@RabbitListener(queues = RabbitMqConfig.EVENT_CLUSTINATOR_FINISHED_QUEUE_NAME, containerFactory = "requeueingContainerFactory")
	public void processClustering(ClustinatorResult result, Channel channel, @Header(AmqpHeaders.CONSUMER_TAG) String consumerTag) throws IOException, TimeoutException {
		try {

			LOGGER.info("{}@{} {}: Received result from clustinator in range {} ({}) - {}", result.getAppId(), result.getVersion(), result.getTailoring(), new Date(result.getStartMicros() / 1000),
					new Date(result.getIntervalStartMicros() / 1000), new Date(result.getEndMicros() / 1000));

			configProvider.waitForInitialization();
			storeClustinatorResult(result);

			LOGGER.info("{}@{} {}: Processing of the clustinator result finished.", result.getAppId(), result.getVersion(), result.getTailoring());
		} catch (Exception e) {
			LOGGER.error("{}@{} {}: {} during processing of the traces!", result.getAppId(), result.getVersion(), result.getTailoring(), e.getClass().getSimpleName());

			if ((result.getAppId() != null) && configProvider.getConfiguration(result.getAppId()).getTraces().isStopOnFailue()) {
				LOGGER.error("{}@{} {} Will stop receiving traces until a restart!", result.getAppId(), result.getVersion(), result.getTailoring());
				status.setActive(false);
				channel.basicCancel(consumerTag);
				throw e;
			} else {
				LOGGER.error("Will ignore the failed traces and continue!");
				throw new AmqpRejectAndDontRequeueException(e);
			}

		}
	}

	private void storeClustinatorResult(ClustinatorResult result) throws IOException, TimeoutException {
		ClustinatorMarkovChainConverter converter = new ClustinatorMarkovChainConverter(result.getStates());
		MarkovBehaviorModel behaviorModel = new MarkovBehaviorModel();
		behaviorModel.setTimestamp(result.getIntervalStartMicros() / 1000);

		for (String group : result.getMeanMarkovChains().keySet()) {

			RelativeMarkovChain behavior = createMarkovChain(group, result, converter);
			behaviorModel.addMarkovChain(behavior);

			updateIntensities(result, group);
		}

		behaviorManager.store(result.getAppId(), result.getTailoring(), behaviorModel);
		LOGGER.info("{}@{} {}: Stored behavior model.", result.getAppId(), result.getVersion(), result.getTailoring());
	}

	private RelativeMarkovChain createMarkovChain(String group, ClustinatorResult result, ClustinatorMarkovChainConverter converter) {
		double[] markovArray = result.getMeanMarkovChains().get(group);
		double[] thinkTimeMeans = result.getThinkTimeMeans().get(group);
		double[] thinkTimeVariances = result.getThinkTimeVariances().get(group);

		RelativeMarkovChain behavior = converter.convertArray(markovArray, thinkTimeMeans, thinkTimeVariances);
		behavior.setFrequency(result.getFrequency().get(group));
		behavior.setId(group);

		return behavior;
	}

	private void updateIntensities(ClustinatorResult result, String group) throws IOException, TimeoutException {
		LOGGER.info("{}@{} {}: Updating the intensities of group {}...", result.getAppId(), result.getVersion(), result.getTailoring(), group);

		CobraConfiguration config = configProvider.getConfiguration(result.getAppId());
		Duration resolution = config.getIntensity().getResolution();
		Duration interval = config.getClustering().getInterval();
		Duration timeout = config.getSessions().getTimeout();
		long resolutionMicros = (resolution.getSeconds() * 1000000) + (resolution.getNano() / 1000);
		long intervalMicros = (interval.getSeconds() * 1000000) + (interval.getNano() / 1000);
		long timeoutMicros = (timeout.getSeconds() * 1000000) + (timeout.getNano() / 1000);

		if ((intervalMicros % resolutionMicros) != 0) {
			LOGGER.warn("The clustering interval {} is not a multiple of the intensity resolution {}. This can lead to unexpected behavior!", interval, resolution);
		}

		IntensityCalculator calculator = new IntensityCalculator(group, resolutionMicros, result.getIntervalStartMicros(), result.getEndMicros(), timeoutMicros);
		sessionManager.scrollForSessionsWithGroupId(result.getAppId(), result.getTailoring(), group, result.getIntervalStartMicros(), result.getEndMicros(), calculator::addSessions, false);

		List<IntensityRecord> intensities = calculator.getRecords();

		if (intensities.size() > 0) {
			try {
				intensityManager.storeOrUpdateIntensities(result.getAppId(), result.getTailoring(), intensities);
				LOGGER.info("{}@{} {}: Updated the intensities of group {}.", result.getAppId(), result.getVersion(), result.getTailoring(), group);
			} catch (IOException e) {
				LOGGER.error("Could not update the intensities!", e);
			}
		} else {
			LOGGER.info("{}@{} {}: There are no intensities of group {} to be updated.", result.getAppId(), result.getVersion(), result.getTailoring(), group);
		}
	}

}
