package org.continuity.cobra.amqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.converter.AccessLogsToOpenXtraceConverter;
import org.continuity.cobra.converter.CsvRowToOpenXtraceConverter;
import org.continuity.cobra.converter.SessionLogsToOpenXtraceConverter;
import org.continuity.cobra.entities.CsvRow;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import open.xtrace.OPENxtraceUtils;

/**
 * Receives measurement data of different types, transforms it to OPEN.xtrace, and forwards it to
 * the {@link IncomingTracesAmqpHandler}.
 *
 * @author Henning Schulz
 *
 */
@Component
public class MeasurementDataTransformationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementDataTransformationAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@RabbitListener(queues = RabbitMqConfig.TASK_TRANSFORM_ACESSLOGS_QUEUE_NAME)
	public void transformAccessLogs(String accessLogs, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey, @Header(AmqpApi.Cobra.HEADER_FINISH) boolean finish) throws IOException {
		Pair<AppId, VersionOrTimestamp> aav = AmqpApi.Cobra.TASK_TRANSFORM_ACCESSLOGS.formatRoutingKey().from(routingKey);

		LOGGER.info("{}@{} Transforming access logs...", aav.getLeft(), aav.getRight());

		List<AccessLogEntry> parsedLogs = new ArrayList<>();

		for (String line : accessLogs.split("\\n")) {
			parsedLogs.add(AccessLogEntry.fromLogLine(line));
		}

		List<Trace> traces = new AccessLogsToOpenXtraceConverter(configProvider.getConfiguration(aav.getLeft()).getSessions().isHashId()).convert(parsedLogs);
		forwardTraces(aav.getLeft(), aav.getRight(), traces, finish);
	}

	@RabbitListener(queues = RabbitMqConfig.TASK_TRANSFORM_CSV_QUEUE_NAME)
	public void transformCsv(String csvContent, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey, @Header(AmqpApi.Cobra.HEADER_FINISH) boolean finish) throws IOException {
		Pair<AppId, VersionOrTimestamp> aav = AmqpApi.Cobra.TASK_TRANSFORM_CSV.formatRoutingKey().from(routingKey);

		LOGGER.info("{}@{} Transforming CSV data...", aav.getLeft(), aav.getRight());

		List<CsvRow> csvRows = CsvRow.listFromString(csvContent);

		List<Trace> traces = new CsvRowToOpenXtraceConverter(configProvider.getConfiguration(aav.getLeft()).getSessions().isHashId()).convert(csvRows);
		forwardTraces(aav.getLeft(), aav.getRight(), traces, finish);
	}

	@RabbitListener(queues = RabbitMqConfig.TASK_TRANSFORM_SESSIONLOGS_QUEUE_NAME)
	public void transformSessionLogs(String sessionContent, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey, @Header(AmqpApi.Cobra.HEADER_FINISH) boolean finish) throws IOException {
		Pair<AppId, VersionOrTimestamp> aav = AmqpApi.Cobra.TASK_TRANSFORM_SESSIONLOGS.formatRoutingKey().from(routingKey);

		LOGGER.info("{}@{} Transforming session logs...", aav.getLeft(), aav.getRight());

		List<String> sessionLogs = Arrays.asList(sessionContent.split("\\n"));

		List<Trace> traces = new SessionLogsToOpenXtraceConverter().convert(sessionLogs);
		forwardTraces(aav.getLeft(), aav.getRight(), traces, finish);
	}

	private void forwardTraces(AppId aid, VersionOrTimestamp version, List<Trace> traces, boolean finish) throws IOException {
		String tracesAsJson = OPENxtraceUtils.serializeTraceListToJsonString(traces);

		amqpTemplate.convertAndSend(AmqpApi.Cobra.TASK_PROCESS_TRACES.name(), AmqpApi.Cobra.TASK_PROCESS_TRACES.formatRoutingKey().of(aid, version), tracesAsJson, AmqpApi.Cobra.finishHeader(finish));

		LOGGER.info("{}@{} Forwarded traces to AMQP handler.", aid, version);
	}

}
