package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.Clustering.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.Clustering.Paths.TRIGGER_LATEST;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.amqp.RoutingKeyFormatter;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.converter.ClustinatorMarkovChainConverter;
import org.continuity.cobra.entities.ClustinatorInput;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(ROOT)
public class ClusteringController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringController.class);

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private ElasticsearchBehaviorManager behaviorManager;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RequestMapping(value = TRIGGER_LATEST, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "tailoring", required = true, dataType = "string", paramType = "path") })
	public void triggerLatestClustering(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable String tailoring,
			@RequestParam(value = "ignore-timeout", defaultValue = "false") boolean ignoreTimeout)
			throws IOException, TimeoutException {
		triggerLatestClustering(aid, Session.convertStringToTailoring(tailoring), ignoreTimeout);
	}

	public void triggerLatestClustering(AppId aid, List<String> tailoring, boolean ignoreTimeout) throws IOException, TimeoutException {
		CobraConfiguration config = configProvider.getConfiguration(aid);

		Duration interval = config.getClustering().getInterval();
		Duration overlap = config.getClustering().getOverlap();
		Duration timeout = config.getSessions().getTimeout();
		long intervalMillis = interval.toMillis();
		long overlapMillis = overlap.toMillis();
		long timeoutMillis = timeout.toMillis();

		Date sessionsEnd = sessionManager.getLatestDate(aid, null, tailoring);
		long clusteringEnd = ignoreTimeout ? sessionsEnd.getTime() : clusteringTimestamp(sessionsEnd, intervalMillis, timeoutMillis);
		long clusteringStart = clusteringEnd - intervalMillis - overlapMillis;
		long intervalStart = clusteringEnd - intervalMillis;

		LOGGER.info("{} {}: Triggering clustering with start {}, interval start {}, end {} (respecting the session timeout of {}) ...", aid, tailoring, new Date(clusteringStart),
				new Date(intervalStart), new Date(clusteringEnd), timeout);

		List<Session> sessions = sessionManager.readSessionsInRange(aid, null, tailoring, new Date(clusteringStart), new Date(clusteringEnd));
		List<String> endpoints = sessions.stream().map(Session::getRequests).flatMap(Set::stream).map(SessionRequest::getEndpoint).distinct().collect(Collectors.toList());
		endpoints.add(0, RelativeMarkovChain.INITIAL_STATE);
		endpoints.add(RelativeMarkovChain.FINAL_STATE);

		MarkovBehaviorModel previousBehavior = behaviorManager.readLatest(aid, tailoring);
		Map<String, double[]> previousMarkovChains;

		if (previousBehavior == null) {
			LOGGER.info("{} {}: There is no previous behavior. Starting from scratch.", aid, tailoring);
			previousMarkovChains = null;
		} else {
			LOGGER.info("{} {}: Using a previous behavior from {}.", aid, tailoring, new Date(previousBehavior.getTimestamp()));
			ClustinatorMarkovChainConverter converter = new ClustinatorMarkovChainConverter(endpoints);
			previousMarkovChains = converter.convertBehaviorModel(previousBehavior);
		}

		ClustinatorInput input = new ClustinatorInput().setAppId(aid).setTailoring(tailoring).setEpsilon(config.getClustering().getEpsilon()).setMinSampleSize(config.getClustering().getMinSampleSize())
				.setStartMicros(clusteringStart * 1000).setIntervalStartMicros(intervalStart * 1000).setEndMicros(clusteringEnd * 1000).setStates(endpoints)
				.setPreviousMarkovChains(previousMarkovChains).setSessions(sessions);

		ExchangeDefinition<RoutingKeyFormatter.AppId> exchange = AmqpApi.Cobra.Clustinator.TASK_CLUSTER;
		amqpTemplate.convertAndSend(exchange.name(), exchange.formatRoutingKey().of(aid), input);

		LOGGER.info("{} {}: Clustering triggered. Waiting for the clustinator.", aid, tailoring);
	}

	private long clusteringTimestamp(Date date, long intervalMillis, long timeoutMillis) {
		return ((date.getTime() - timeoutMillis) / intervalMillis) * intervalMillis;
	}

}