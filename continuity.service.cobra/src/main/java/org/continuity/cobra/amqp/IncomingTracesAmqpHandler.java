package org.continuity.cobra.amqp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
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
import org.continuity.cobra.controllers.ClusteringController;
import org.continuity.cobra.converter.AccessLogsToOpenXtraceConverter;
import org.continuity.cobra.converter.CsvRowToOpenXtraceConverter;
import org.continuity.cobra.converter.SessionLogsToOpenXtraceConverter;
import org.continuity.cobra.entities.CsvRow;
import org.continuity.cobra.entities.TraceProcessingStatus;
import org.continuity.cobra.entities.TraceRecord;
import org.continuity.cobra.extractor.RequestTailorer;
import org.continuity.cobra.extractor.SessionUpdater;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.HTTPMethod;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.rabbitmq.client.Channel;

import open.xtrace.OPENxtraceUtils;

/**
 * Receives newly uploaded data and processes it.
 *
 * @author Henning Schulz
 *
 */
@Component
public class IncomingTracesAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(IncomingTracesAmqpHandler.class);

	private static final long SECONDS_TO_MICROS = 1000 * 1000;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ElasticsearchTraceManager traceManager;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

	@Autowired
	private ClusteringController clusteringController;

	@Autowired
	private TraceProcessingStatus status;

	/**
	 * Receives and processes new traces.
	 *
	 * @param message
	 *            The message (instead of typical payload object for custom OPEN.xtrace
	 *            deserialization).
	 * @param routingKey
	 *            The routing key used by the sender.
	 * @param finish
	 *            Whether to finish all sessions created out of the traces.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_PROCESS_TRACES_QUEUE_NAME, containerFactory = "incomingTracesContainerFactory")
	public void processTraces(Message message, Channel channel, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey, @Header(AmqpHeaders.CONSUMER_TAG) String consumerTag,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(AmqpApi.Cobra.HEADER_DATATYPE) String datatype, @Header(AmqpApi.Cobra.HEADER_FINISH) boolean finish)
			throws IOException, TimeoutException {

		Pair<AppId, VersionOrTimestamp> aav = Pair.of(null, null);

		try {

			aav = AmqpApi.Cobra.TASK_PROCESS_TRACES.formatRoutingKey().from(routingKey);
			configProvider.waitForInitialization();

			doProcessing(message, aav, datatype, finish);

		} catch (Exception e) {
			LOGGER.error("{}@{} {} during processing of the traces!", aav.getLeft(), aav.getRight(), e.getClass().getSimpleName());

			if ((aav.getLeft() != null) && configProvider.getConfiguration(aav.getLeft()).getTraces().isStopOnFailue()) {
				LOGGER.error("{}@{} Will stop receiving traces until a restart!", aav.getLeft(), aav.getRight());
				status.setActive(false);
				channel.basicCancel(consumerTag);
				throw e;
			} else {
				LOGGER.error("Will ignore the failed traces and continue!");
				throw new AmqpRejectAndDontRequeueException(e);
			}

		}
	}

	private void doProcessing(Message message, Pair<AppId, VersionOrTimestamp> aav, String datatype, boolean finish) throws IOException, TimeoutException {
		long startMillis = System.currentTimeMillis();
		LOGGER.info("{}@{}: Processing new traces.", aav.getLeft(), aav.getRight());

		if (!aav.getLeft().isAll()) {
			LOGGER.warn("Dropping service of app-id {}.", aav.getLeft());
		}

		if (finish) {
			LOGGER.info("{}@{}: Finishing the sessions is forced.");
		}

		AppId aid = aav.getLeft().dropService();
		VersionOrTimestamp version = aav.getRight();

		List<TraceRecord> traces = convertMessage(message, datatype, aid, version).stream().map(t -> new TraceRecord(version, t)).collect(Collectors.toList());

		LOGGER.info("{}@{}: Deserialized {} traces. Indexing with endpoints...", aid, version, traces.size());

		indexTracesWithEndpoints(aid, version, traces);

		LOGGER.info("{}@{}: Indexing done. Grouping to sessions...", aid, version);

		if (configProvider.getConfiguration(aid).getSessions().isOmit()) {
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

	private List<Trace> convertMessage(Message message, String datatype, AppId aid, VersionOrTimestamp version) {
		Charset charset = Charset.forName(message.getMessageProperties().getContentEncoding());

		if (charset == null) {
			charset = AmqpApi.Cobra.CONTENT_CHARSET;
		}

		String body = new String(message.getBody(), charset);

		switch (datatype) {
		case "access-logs":
			return convertAccesslogs(body, aid, version);
		case "csv":
			return convertCsv(body, aid, version);
		case "session-logs":
			return convertSessionlogs(body, aid, version);
		case "open-xtrace":
		default:
			return deserializeOpenXtrace(body, aid, version);
		}
	}

	private List<Trace> convertAccesslogs(String body, AppId aid, VersionOrTimestamp version) {
		LOGGER.info("{}@{} Transforming access logs to open-xtrace...", aid, version);

		List<AccessLogEntry> parsedLogs = new ArrayList<>();

		for (String line : body.split("\\n")) {
			parsedLogs.add(AccessLogEntry.fromLogLine(line));
		}

		return new AccessLogsToOpenXtraceConverter(configProvider.getConfiguration(aid).getSessions().isHashId()).convert(parsedLogs);
	}

	private List<Trace> convertCsv(String body, AppId aid, VersionOrTimestamp version) {
		LOGGER.info("{}@{} Transforming CSV data to open-xtrace...", aid, version);

		List<CsvRow> csvRows = CsvRow.listFromString(body);
		return new CsvRowToOpenXtraceConverter(configProvider.getConfiguration(aid).getSessions().isHashId()).convert(csvRows);
	}

	private List<Trace> convertSessionlogs(String body, AppId aid, VersionOrTimestamp version) {
		LOGGER.info("{}@{} Transforming session logs to open-xtrace...", aid, version);

		List<String> sessionLogs = Arrays.asList(body.split("\\n"));
		return new SessionLogsToOpenXtraceConverter().convert(sessionLogs);
	}

	private List<Trace> deserializeOpenXtrace(String body, AppId aid, VersionOrTimestamp version) {
		LOGGER.info("{}@{} Deserializing open-xtrace...", aid, version);

		return OPENxtraceUtils.deserializeIntoTraceList(body);
	}

	private void storeTraces(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces) throws IOException {
		CobraConfiguration config = configProvider.getConfiguration(aid);

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

		long retention = config.getTraces().getRetention().toMillis();

		if (retention > 0) {
			if (to != null) {
				long before = to.getTime() - retention;

				if (before > 0) {
					LOGGER.info("{}@{}: Deleting the traces before {}.", aid, version, new Date(before));
					traceManager.deleteTracesBefore(aid, new Date(before));
				}
			}

			traceManager.storeTraceRecords(aid, version, traces);
		} else {
			LOGGER.info("{}@{}: Not storing the traces by configuration.", aid, version);
		}

		LOGGER.info("{}@{}: Traces range from {} to {}.", aid, version, from, to);
	}

	private void groupSessions(AppId aid, VersionOrTimestamp version, List<TraceRecord> traces, boolean forceFinish) throws IOException, TimeoutException {
		RequestTailorer tailorer = new RequestTailorer(aid, version, restTemplate);
		CobraConfiguration config = configProvider.getConfiguration(aid);

		List<List<String>> tailoring = config.getTailoring();

		if (!config.getTraces().isMapToIdpa()) {
			LOGGER.info("{}@{}: Not mapping to an IDPA. Therefore, ignoring all configured service combinations for tailoring and using [ \"all\" ].", aid, version);
			tailoring = Collections.singletonList(Collections.singletonList(AppId.SERVICE_ALL));
		}

		for (List<String> services : tailoring) {
			LOGGER.info("{}@{}: Tailoring to {}...", aid, version, services);

			List<SessionRequest> requests;

			if (config.getTraces().isMapToIdpa()) {
				requests = tailorer.tailorTraces(services, traces);
			} else {
				requests = tailorer.tailorTracesWithoutMapping(traces);
			}

			List<Session> openSessions = sessionManager.readOpenSessions(aid, null, services);

			SessionUpdater updater = new SessionUpdater(version, config.getSessions().getTimeout().getSeconds() * SECONDS_TO_MICROS, forceFinish, config.getSessions().isIgnoreRedirects());
			Set<Session> updatedSessions = updater.updateSessions(openSessions, requests);

			if (!updatedSessions.isEmpty()) {
				LOGGER.info("{}@{} {}: Indexing traces with sessions...", aid, version, services);
				indexTracesWithSessions(traces, updatedSessions);

				Date latestDateBeforeUpdate = sessionManager.getLatestDate(aid, null, services);

				LOGGER.info("{}@{} {}: Storing sessions...", aid, version, services);
				sessionManager.storeOrUpdateSessions(aid, updatedSessions, services, true);

				Date latestDateAfterUpdate = sessionManager.getLatestDate(aid, null, services);
				Date startDateOfSessions = sessionManager.getEarliestDate(aid, null, services);

				LOGGER.info("{}@{} {}: Sessions stored. Latest date before update: {}, start of new sessions: {}, and latest date after: {}.", aid, version, services, latestDateBeforeUpdate,
						startDateOfSessions, latestDateAfterUpdate);

				triggerClustering(aid, version, services, startDateOfSessions, latestDateBeforeUpdate, latestDateAfterUpdate, forceFinish);
			} else {
				LOGGER.info("{}@{} {}: No sessions have been updated.", aid, version, services);
			}
		}
	}

	private void triggerClustering(AppId aid, VersionOrTimestamp version, List<String> services, Date startDateOfSessions, Date latestDateBeforeUpdate, Date latestDateAfterUpdate, boolean forceFinish)
			throws IOException, TimeoutException {
		CobraConfiguration config = configProvider.getConfiguration(aid);

		Duration interval = config.getClustering().getInterval();
		Duration timeout = config.getSessions().getTimeout();
		long intervalMillis = interval.toMillis();
		long timeoutMillis = timeout.toMillis();

		long lastClusteringEnd = clusteringTimestamp(latestDateBeforeUpdate, intervalMillis, timeoutMillis);
		long clusteringEnd = forceFinish ? latestDateAfterUpdate.getTime() : clusteringTimestamp(latestDateAfterUpdate, intervalMillis, timeoutMillis);

		if (config.getClustering().isOmit()) {
			LOGGER.info("{}@{} {}: Clustering is omitted by configuration.", aid, version, services);
		} else if ((clusteringEnd <= lastClusteringEnd) || (clusteringEnd <= startDateOfSessions.getTime())) {
			LOGGER.info("{}@{} {}: Clustering is not due, yet. Next will be after upload of: {}.", aid, version, services, new Date(clusteringEnd + intervalMillis + timeoutMillis));
		} else {
			clusteringController.triggerLatestClustering(aid, services, forceFinish);
		}
	}

	private long clusteringTimestamp(Date date, long intervalMillis, long timeoutMillis) {
		return ((date.getTime() - timeoutMillis) / intervalMillis) * intervalMillis;
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
		CobraConfiguration config = configProvider.getConfiguration(aid);
		boolean discard = config.getTraces().isDiscardUmapped();

		Set<String> unmapped = new HashSet<>();
		int numUnmapped = 0;

		BiFunction<TraceRecord, HTTPRequestProcessingImpl, Boolean> endpointSetter;

		if (config.getTraces().isMapToIdpa()) {
			Application rootApp;
			try {
				rootApp = restTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl(aid).withQuery("version", version.toString()).get(), Application.class);
			} catch (HttpStatusCodeException e) {
				LOGGER.error("Could not get root application for app-id {} and version {}! {} ({}): {}", aid, version, e.getStatusCode(), e.getStatusCode().getReasonPhrase(),
						e.getResponseBodyAsString());
				return;
			}

			RequestUriMapper rootMapper = new RequestUriMapper(rootApp);

			endpointSetter = (trace, callable) -> {
				HttpEndpoint endpoint = rootMapper.map(callable.getUri(), callable.getRequestMethod().get().name());

				if (endpoint != null) {
					trace.setRawEndpoint(endpoint);
				}

				return endpoint != null;
			};
		} else {
			endpointSetter = (trace, callable) -> {
				Optional<String> bt = OPENxtraceUtils.getBusinessTransaction(callable);

				if (bt.isPresent() && !bt.get().isEmpty()) {
					trace.setEndpoint(bt.get());
					return true;
				} else {
					return false;
				}
			};
		}

		ListIterator<TraceRecord> iterator = traces.listIterator();

		while (iterator.hasNext()) {
			TraceRecord trace = iterator.next();

			List<HTTPRequestProcessingImpl> rootCallables = OpenXtraceTracer.forRoot(trace.getTrace().getRoot().getRoot()).extractSubtraces();

			if (rootCallables.size() == 0) {
				LOGGER.warn("Trace {} does not contain HTTPRequestprocessings. Cannot set endpoint to TraceRecord!", trace.getTrace().getTraceId());
				iterator.remove();
				continue;
			}

			boolean endpointSet = endpointSetter.apply(trace, rootCallables.get(0));

			if (!endpointSet) {
				String method = rootCallables.get(0).getRequestMethod().map(HTTPMethod::name).orElse("?");
				String path = rootCallables.get(0).getUri();
				unmapped.add(new StringBuilder().append(method).append(" ").append(path).toString());

				numUnmapped++;

				if (discard) {
					iterator.remove();
				}
			}
		}

		if (config.getTraces().isLogUnmapped() && !unmapped.isEmpty()) {
			try {
				Files.write(Paths.get(toUnmappedFilename(aid)), unmapped);
			} catch (IOException e) {
				LOGGER.error("Could not store unmapped log file!", e);
			}
		}

		if (numUnmapped > 50) {
			LOGGER.warn("{}@{}: Could not find an endpoint for {} traces with {} endpoints!", aid, version, numUnmapped, unmapped.size());
		} else if (!unmapped.isEmpty()) {
			LOGGER.warn("{}@{}: Could not find an endpoint for {} traces with the following endpoints: {}", aid, version, unmapped.size(), unmapped);
		} else {
			LOGGER.info("{}@{}: All traces have been mapped to endpoints successfully.", aid, version);
		}

		if (!unmapped.isEmpty() && discard) {
			LOGGER.info("{}@{}: The traces without endpoint won't be stored.", aid, version);
		}
	}

	private String toUnmappedFilename(AppId aid) {
		return new StringBuilder().append(LocalDateTime.now()).append(".").append(aid).append(".unmapped.txt").toString();
	}

}
