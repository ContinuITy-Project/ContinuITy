package org.continuity.cobra.amqp;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovTransition;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.converter.ClustinatorMarkovChainConverter;
import org.continuity.cobra.entities.ClustinatorResult;
import org.continuity.cobra.extractor.IntensityCalculator;
import org.continuity.cobra.extractor.SessionsToMarkovChainAggregator;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.dsl.timeseries.IntensityRecord;
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

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@Autowired
	private ElasticsearchBehaviorManager behaviorManager;

	@Autowired
	private ElasticsearchIntensityManager intensityManager;

	private SessionsToMarkovChainAggregator aggregator = new SessionsToMarkovChainAggregator();

	@RabbitListener(queues = RabbitMqConfig.EVENT_CLUSTINATOR_FINISHED_QUEUE_NAME)
	public void processClustering(ClustinatorResult result) throws IOException, TimeoutException {
		LOGGER.info("{}@{} {}: Received result from clustinator in range {} ({}) - {}", result.getAppId(), result.getVersion(), result.getTailoring(), new Date(result.getStartMicros() / 1000),
				new Date(result.getIntervalStartMicros() / 1000), new Date(result.getEndMicros() / 1000));

		storeClustinatorResult(result);

		LOGGER.info("{}@{} {}: Processing of the clustinator result finished.", result.getAppId(), result.getVersion(), result.getTailoring());
	}

	private void storeClustinatorResult(ClustinatorResult result) throws IOException, TimeoutException {
		ClustinatorMarkovChainConverter converter = new ClustinatorMarkovChainConverter(result.getStates());
		MarkovBehaviorModel behaviorModel = new MarkovBehaviorModel();
		behaviorModel.setTimestamp(result.getIntervalStartMicros() / 1000);

		double numSessions = result.getSessions().values().stream().flatMap(Arrays::stream).count();

		for (String group : result.getMeanMarkovChains().keySet()) {
			List<Session> sessions = sessionManager.readSessionsWithId(result.getAppId(), result.getSessions().get(group));

			RelativeMarkovChain behavior = createMarkovChain(group, result, sessions, converter, numSessions);
			behaviorModel.addMarkovChain(behavior);

			updateIntensities(result, group, sessions);
		}

		behaviorManager.store(result.getAppId(), result.getTailoring(), behaviorModel);
		LOGGER.info("{}@{} {}: Stored behavior model.", result.getAppId(), result.getVersion(), result.getTailoring());
	}

	private RelativeMarkovChain createMarkovChain(String group, ClustinatorResult result, List<Session> sessions, ClustinatorMarkovChainConverter converter, double numSessions) {
		double[] markovArray = result.getMeanMarkovChains().get(group);
		String[] sessionIds = result.getSessions().get(group);

		RelativeMarkovChain behavior = converter.convertArray(markovArray);
		behavior.setFrequency(sessionIds.length / numSessions);
		behavior.setId(group);

		RelativeMarkovChain thinkTimeChain = aggregator.aggregate(sessions);

		for (String from : result.getStates()) {
			for (String to : result.getStates()) {
				NormalDistribution thinkTime = thinkTimeChain.getTransition(from, to).getThinkTime();

				if ((thinkTime.getMean() > 0) || (thinkTime.getVariance() > 0)) {
					RelativeMarkovTransition transition = behavior.getTransition(from, to);
					transition.setThinkTime(thinkTime);
					behavior.setTransition(from, to, transition);
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			compareBehaviorModels(result.getStates(), behavior, thinkTimeChain);
		}

		return behavior;
	}

	private void compareBehaviorModels(List<String> states, RelativeMarkovChain returnedChain, RelativeMarkovChain calculatedChain) {
		LOGGER.debug("Comparing the calculated and returned transitions of group {}...", returnedChain.getId());

		for (String from : states) {
			for (String to : states) {
				double returned = returnedChain.getTransition(from, to).getProbability();
				double calculated = calculatedChain.getTransition(from, to).getProbability();
				double diff = Math.abs(returned - calculated);

				if (diff > 0.001) {
					LOGGER.debug("Markov chain {}: Calculated transition ({} -> {}) differs from returned one: |{} - {}| = {}", returnedChain.getId(), from, to, DECIMAL_FORMAT.format(calculated),
							DECIMAL_FORMAT.format(returned), DECIMAL_FORMAT.format(diff));
				}
			}
		}

		LOGGER.debug("Comparison of group {} done.", returnedChain.getId());
	}

	private void updateIntensities(ClustinatorResult result, String group, List<Session> sessions) {
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
		List<IntensityRecord> intensities = calculator.calculate(sessions);

		try {
			intensityManager.storeOrUpdateIntensities(result.getAppId(), result.getTailoring(), intensities);
			LOGGER.info("{}@{} {}: Updated the intensities of group {}.", result.getAppId(), result.getVersion(), result.getTailoring(), group);
		} catch (IOException e) {
			LOGGER.error("Could not update the intensities!", e);
		}
	}

}
