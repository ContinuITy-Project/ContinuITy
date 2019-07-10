package org.continuity.session.logs.amqp;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.session.logs.SessionLogsConfiguration;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.session.logs.config.RabbitMqConfig;
import org.continuity.session.logs.extractor.RequestTailorer;
import org.continuity.session.logs.extractor.SessionUpdater;
import org.continuity.session.logs.managers.ElasticsearchSessionManager;
import org.continuity.session.logs.managers.ElasticsearchTraceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
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
	private ConfigurationProvider<SessionLogsConfiguration> configProvider;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ElasticsearchTraceManager traceManager;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@RabbitListener(queues = RabbitMqConfig.TASK_PROCESS_TRACES_QUEUE_NAME)
	public void processTraces(String tracesAsJson, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws IOException, TimeoutException {
		Pair<AppId, VersionOrTimestamp> aav = AmqpApi.SessionLogs.TASK_PROCESS_TRACES.formatRoutingKey().from(routingKey);

		long startMillis = System.currentTimeMillis();
		LOGGER.info("{}@{}: Processing new traces. Deserializing...", aav.getLeft(), aav.getRight());

		if (!aav.getLeft().isAll()) {
			LOGGER.warn("Dropping service of app-id {}.", aav.getLeft());
		}

		AppId aid = aav.getLeft().dropService();
		VersionOrTimestamp version = aav.getRight();

		List<Trace> traces = OPENxtraceUtils.deserializeIntoTraceList(tracesAsJson);

		LOGGER.info("{}@{}: Deserialization done. Storing the traces to the database...", aid, version);

		storeTraces(aid, version, traces);

		LOGGER.info("{}@{}: Storing done.", aid, version);

		if (configProvider.getOrDefault(aid).isOmitSessionClustering()) {
			LOGGER.info("{}@{}: Clustering is omitted by configuration.", aid, version);
		} else {
			LOGGER.info("{}@{}: Clustering and updating the corresponding sessions...", aid, version);
			clusterSessions(aid, version, traces);
		}

		long endMillis = System.currentTimeMillis();
		LOGGER.info("{}@{}: Processing of the traces done. It took {}", aid, version, DurationFormatUtils.formatDurationHMS(endMillis - startMillis));
	}

	private void storeTraces(AppId aid, VersionOrTimestamp version, List<Trace> traces) throws IOException {
		traceManager.storeTraces(aid, version, traces);

		Date from = null;
		Date to = null;

		OptionalLong fromOpt = traces.stream().mapToLong(t -> t.getRoot().getRoot().getTimestamp()).min();
		OptionalLong toOpt = traces.stream().mapToLong(t -> t.getRoot().getRoot().getTimestamp()).max();

		if (fromOpt.isPresent()) {
			from = new Date(fromOpt.getAsLong() - 1);
		}

		if (toOpt.isPresent()) {
			to = new Date(toOpt.getAsLong());
		}

		LOGGER.info("{}@{}: Traces range from {} to {}.", aid, version, from, to);
	}

	private void clusterSessions(AppId aid, VersionOrTimestamp version, List<Trace> traces) throws IOException, TimeoutException {
		RequestTailorer tailorer = new RequestTailorer(aid, version, restTemplate);
		SessionLogsConfiguration config = configProvider.getOrDefault(aid);

		for (List<String> services : config.getTailoring()) {
			LOGGER.info("{}@{}: Tailoring to {}...", aid, version, services);

			List<SessionRequest> requests = tailorer.tailorTraces(services, traces);
			List<Session> openSessions = sessionManager.readOpenSessions(aid, version, services);

			SessionUpdater updater = new SessionUpdater(version, services, config.getMaxSessionPause().getSeconds() * SECONDS_TO_MICROS);
			Set<Session> updatedSessions = updater.updateSessions(openSessions, requests);

			if (!updatedSessions.isEmpty()) {
				sessionManager.storeOrUpdateSessions(aid, updatedSessions);
			} else {
				LOGGER.info("{}@{} {} No sessions have been updated.", aid, version, services);
			}
		}
	}

}
