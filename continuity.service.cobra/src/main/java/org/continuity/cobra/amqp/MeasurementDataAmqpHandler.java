package org.continuity.cobra.amqp;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.api.rest.RestApi;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.entities.TraceRecord;
import org.continuity.cobra.extractor.RequestTailorer;
import org.continuity.cobra.extractor.SessionUpdater;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import open.xtrace.OPENxtraceUtils;

/**
 * Receives newly uploaded data and processes it.
 *
 * @author Henning Schulz
 *
 */
@Component
public class MeasurementDataAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementDataAmqpHandler.class);

	private static final long SECONDS_TO_MICROS = 1000 * 1000;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ElasticsearchTraceManager traceManager;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@RabbitListener(queues = RabbitMqConfig.TASK_PROCESS_TRACES_QUEUE_NAME)
	public void processTraces(String tracesAsJson, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey, @Header(RabbitMqConfig.HEADER_FINISH) boolean finish)
			throws IOException, TimeoutException {
		Pair<AppId, VersionOrTimestamp> aav = AmqpApi.Cobra.TASK_PROCESS_TRACES.formatRoutingKey().from(routingKey);

		long startMillis = System.currentTimeMillis();
		LOGGER.info("{}@{}: Processing new traces. Deserializing...", aav.getLeft(), aav.getRight());

		if (!aav.getLeft().isAll()) {
			LOGGER.warn("Dropping service of app-id {}.", aav.getLeft());
		}

		AppId aid = aav.getLeft().dropService();
		VersionOrTimestamp version = aav.getRight();

		List<TraceRecord> traces = OPENxtraceUtils.deserializeIntoTraceList(tracesAsJson).stream().map(t -> new TraceRecord(version, t)).collect(Collectors.toList());

		LOGGER.info("{}@{}: Deserialization done. Indexing with endpoints...", aid, version);

		indexTracesWithEndpoints(aid, version, traces);

		LOGGER.info("{}@{}: Indexing done. Grouping to sessions...", aid, version);

		if (configProvider.getOrDefault(aid).isOmitSessionClustering()) {
			LOGGER.info("{}@{}: Session grouping and clustering is omitted by configuration.", aid, version);
		} else {
			LOGGER.info("{}@{}: Grouping and updating the corresponding sessions...", aid, version);
			groupSessions(aid, version, traces, finish);
		}

		LOGGER.info("{}@{}: Storing the traces to the database...", aid, version);

		storeTraces(aid, version, traces);

		LOGGER.info("{}@{}: Storing done.", aid, version);

		long endMillis = System.currentTimeMillis();
		LOGGER.info("{}@{}: Processing of the traces done. It took {}", aid, version, DurationFormatUtils.formatDurationHMS(endMillis - startMillis));
	}

	private void storeTraces(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces) throws IOException {
		traceManager.storeTraceRecords(aid, version, traces);

		Date from = null;
		Date to = null;

		OptionalLong fromOpt = traces.stream().mapToLong(t -> t.getTrace().getRoot().getRoot().getTimestamp()).min();
		OptionalLong toOpt = traces.stream().mapToLong(t -> t.getTrace().getRoot().getRoot().getTimestamp()).max();

		if (fromOpt.isPresent()) {
			from = new Date(fromOpt.getAsLong() - 1);
		}

		if (toOpt.isPresent()) {
			to = new Date(toOpt.getAsLong());
		}

		LOGGER.info("{}@{}: Traces range from {} to {}.", aid, version, from, to);
	}

	private void groupSessions(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces, boolean forceFinish) throws IOException, TimeoutException {
		RequestTailorer tailorer = new RequestTailorer(aid, version, restTemplate);
		CobraConfiguration config = configProvider.getOrDefault(aid);

		for (List<String> services : config.getTailoring()) {
			LOGGER.info("{}@{}: Tailoring to {}...", aid, version, services);

			List<SessionRequest> requests = tailorer.tailorTraces(services, traces);
			List<Session> openSessions = sessionManager.readOpenSessions(aid, version, services);

			SessionUpdater updater = new SessionUpdater(version, config.getMaxSessionPause().getSeconds() * SECONDS_TO_MICROS, forceFinish);
			Set<Session> updatedSessions = updater.updateSessions(openSessions, requests);


			if (!updatedSessions.isEmpty()) {
				LOGGER.info("{}@{} {}: Indexing traces with sessions...", aid, version, services);
				indexTracesWithSessions(traces, updatedSessions);

				LOGGER.info("{}@{} {}: Storing sessions...", aid, version, services);
				sessionManager.storeOrUpdateSessions(aid, updatedSessions, services);
			} else {
				LOGGER.info("{}@{} {}: No sessions have been updated.", aid, version, services);
			}
		}
	}

	private void indexTracesWithSessions(List<TraceRecord> traces, Set<Session> sessions) {
		Map<Long, Set<String>> sessionMap = createSessionMap(sessions);

		for (TraceRecord trace : traces) {
			trace.addUniqueSessionIds(sessionMap.get(trace.getTrace().getTraceId()));
		}
	}

	private Map<Long, Set<String>> createSessionMap(Set<Session> sessions) {
		Map<Long, Set<String>> sessionMap = new HashMap<>();

		for (Session session : sessions) {
			for (SessionRequest request : session.getRequests()) {
				long traceId = request.getTraceId();
				Set<String> sids = sessionMap.get(traceId);

				if (sids == null) {
					sids = new HashSet<>();
					sessionMap.put(traceId, sids);
				}

				sids.add(session.getUniqueId());
			}
		}

		return sessionMap;
	}

	private void indexTracesWithEndpoints(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces) {
		Application rootApp;
		try {
			rootApp = restTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl(aid).withQuery("version", version.toString()).get(), Application.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not get root application for app-id {} and version {}! {} ({}): {}", aid, version, e.getStatusCode(), e.getStatusCode().getReasonPhrase(),
					e.getResponseBodyAsString());
			return;
		}

		RequestUriMapper rootMapper = new RequestUriMapper(rootApp);

		for (TraceRecord trace : traces) {
			List<HTTPRequestProcessingImpl> rootCallables = OpenXtraceTracer.forRoot(trace.getTrace().getRoot().getRoot()).extractSubtraces();

			if (rootCallables.size() == 0) {
				LOGGER.warn("Trace {} does not contain HTTPRequestprocessings. Cannot set endpoint to TraceRecord!", trace.getTrace().getTraceId());
			}

			HttpEndpoint endpoint = rootMapper.map(rootCallables.get(0).getUri(), rootCallables.get(0).getRequestMethod().get().name());
			trace.setRawEndpoint(endpoint);
		}
	}

}
