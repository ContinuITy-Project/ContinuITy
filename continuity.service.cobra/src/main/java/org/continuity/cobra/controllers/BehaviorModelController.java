package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.BehaviorModel.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.BehaviorModel.Paths.CREATE;
import static org.continuity.api.rest.RestApi.Cobra.BehaviorModel.Paths.GET_LATEST;
import static org.continuity.api.rest.RestApi.Cobra.BehaviorModel.Paths.UPDATE_NUM_SESSIONS;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.SessionTailoringDescription;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.entities.TraceRecord;
import org.continuity.cobra.extractor.RequestTailorer;
import org.continuity.cobra.extractor.SessionUpdater;
import org.continuity.cobra.extractor.SessionsToMarkovChainAggregator;
import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class BehaviorModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorModelController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ElasticsearchTraceManager traceManager;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@Autowired
	private ElasticsearchBehaviorManager behaviorManager;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@RequestMapping(value = GET_LATEST, method = RequestMethod.GET, produces = "application/json")
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<MarkovBehaviorModel> getLatestBehaviorModel(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, Long before) throws IOException, TimeoutException {
		MarkovBehaviorModel model;

		if (before == null) {
			model = behaviorManager.readLatest(aid, Session.convertStringToTailoring(tailoring));
		} else {
			model = behaviorManager.readLatest(aid, Session.convertStringToTailoring(tailoring), before);
		}

		if (model == null) {
			return ResponseEntity.notFound().build();
		} else {
			model.sanitizeProbabilities();
			return ResponseEntity.ok(model);
		}
	}

	@RequestMapping(value = CREATE, method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<RelativeMarkovChain> getTailoredMarkovChainAsJson(@RequestBody SessionTailoringDescription description) throws IOException, TimeoutException {
		return ResponseEntity.ok(getTailoredMarkovChain(description));
	}

	@RequestMapping(value = CREATE, method = RequestMethod.POST, produces = "text/plain")
	public ResponseEntity<String> getTailoredMarkovChainAsMatrix(@RequestBody SessionTailoringDescription description) throws IOException, TimeoutException {
		RelativeMarkovChain chain = getTailoredMarkovChain(description);
		String matrix = Arrays.stream(chain.toCsv()).map(Arrays::stream).map(s -> s.collect(Collectors.joining(","))).collect(Collectors.joining("\n"));
		return ResponseEntity.ok(matrix);
	}

	@RequestMapping(value = UPDATE_NUM_SESSIONS, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public void updateNumSessions(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @PathVariable long timestamp) throws IOException, TimeoutException {
		List<String> lTailoring = Session.convertStringToTailoring(tailoring);

		CobraConfiguration config = configProvider.getConfiguration(aid);
		long interval = config.getClustering().getInterval().toMillis();
		long overlap = config.getClustering().getAppend().getStrategy().includeOverlap() ? config.getClustering().getOverlap().toMillis() : 0;

		long clusteringStart = timestamp - overlap;
		long clusteringEnd = timestamp + interval;

		MarkovBehaviorModel behaviorModel = behaviorManager.readLatest(aid, lTailoring, timestamp);
		int numGroups = behaviorModel.getMarkovChains().size();

		Map<String, Long> numSessions = sessionManager.countSessionsPerGroupInRange(aid, null, lTailoring, new Date(clusteringStart), new Date(clusteringEnd), numGroups);

		for (RelativeMarkovChain chain : behaviorModel.getMarkovChains()) {
			chain.setNumSessions(numSessions.getOrDefault(chain.getId(), 0L));
		}

		behaviorManager.store(aid, lTailoring, behaviorModel);
	}

	private RelativeMarkovChain getTailoredMarkovChain(SessionTailoringDescription description) throws IOException, TimeoutException {
		AppId aid = description.getAid();
		String rootEndpoint = description.getRootEndpoint();
		VersionOrTimestamp version = description.getVersion();
		boolean includePrePost = description.isIncludePrePostProcessing();
		List<String> services = description.getTailoring();

		LOGGER.info("Generating tailored Markov chain for app-id {}, root endpoint {}, version {}, and services {}...", aid, rootEndpoint, version, services);

		List<TraceRecord> traces = traceManager.readTraceRecords(aid, rootEndpoint, description.getSessionIds());

		RequestTailorer tailorer = new RequestTailorer(aid, version, restTemplate, includePrePost);
		SessionUpdater updater = new SessionUpdater(version, Long.MAX_VALUE, true, configProvider.getConfiguration(aid).getSessions().isIgnoreRedirects());
		SessionsToMarkovChainAggregator aggregator = new SessionsToMarkovChainAggregator();

		List<SessionRequest> requests = tailorer.tailorTraces(services, traces);
		Set<Session> sessions = updater.updateSessions(Collections.emptyList(), requests);
		RelativeMarkovChain chain = aggregator.aggregate(sessions);

		if (includePrePost) {
			removePrePostProcessingState(SessionRequest.PREFIX_PRE_PROCESSING, chain);
			removePrePostProcessingState(SessionRequest.PREFIX_POST_PROCESSING, chain);
		}

		LOGGER.info("Tailoring for app-id {}, version {}, root endpoint {}, and services {} done.", aid, rootEndpoint, version, services);

		return chain;
	}

	private void removePrePostProcessingState(String prefix, RelativeMarkovChain chain) {
		int removed = chain.removeStates(s -> s.startsWith(prefix), NormalDistribution.ZERO);

		if (removed != 1) {
			LOGGER.warn("Expected to remove 1 state with prefix {} but were {} states actually!", prefix, removed);
		}
	}

}
