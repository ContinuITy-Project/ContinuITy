package org.continuity.session.logs.controllers;

import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.ROOT;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.GET;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.GET_VERSION;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_ACCESS_LOGS;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_CSV;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_LINK;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_OPEN_XTRACE;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.TimeoutException;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.MeasurementDataSpec;
import org.continuity.api.entities.config.session.logs.SessionLogsConfiguration;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.session.logs.converter.AccessLogsToOpenXtraceConverter;
import org.continuity.session.logs.converter.CsvRowToOpenXtraceConverter;
import org.continuity.session.logs.entities.CsvRow;
import org.continuity.session.logs.managers.ElasticsearchTraceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import open.xtrace.OPENxtraceUtils;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for storing and retrieving measurement data such as OPEN.xtrace or access logs.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class MeasurementDataController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementDataController.class);

	@Autowired
	@Qualifier("plainRestTemplate")
	private RestTemplate plainRestTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ElasticsearchTraceManager manager;

	@Autowired
	private ConfigurationProvider<SessionLogsConfiguration> configProvider;

	@RequestMapping(value = GET, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getTraces(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam(required = false) String from, @RequestParam(required = false) String to)
			throws IOException, TimeoutException {
		Date dFrom = null;

		if (from != null) {
			try {
				dFrom = ApiFormats.DATE_FORMAT.parse(from);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse from date!", e);
				return ResponseEntity.badRequest().body("Illegal date format of 'from' date: " + from);
			}
		}

		Date dTo = null;

		if (to != null) {
			try {
				dTo = ApiFormats.DATE_FORMAT.parse(to);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse to date!", e);
				return ResponseEntity.badRequest().body("Illegal date format of 'to' date: " + to);
			}
		}

		List<Trace> traces = manager.readTraces(aid, null, dFrom, dTo);
		String json = OPENxtraceUtils.serializeTraceListToJsonString(traces);

		return ResponseEntity.ok(json);
	}

	@RequestMapping(value = GET_VERSION, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getTracesForVersion(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version,
			@RequestParam(required = false) String from, @RequestParam(required = false) String to) throws IOException, TimeoutException {
		Date dFrom = null;

		if (from != null) {
			try {
				dFrom = ApiFormats.DATE_FORMAT.parse(from);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse from date!", e);
				return ResponseEntity.badRequest().body("Illegal date format of 'from' date: " + from);
			}
		}

		Date dTo = null;

		if (to != null) {
			try {
				dTo = ApiFormats.DATE_FORMAT.parse(to);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse to date!", e);
				return ResponseEntity.badRequest().body("Illegal date format of 'to' date: " + to);
			}
		}

		List<Trace> traces = manager.readTraces(aid, version, dFrom, dTo);
		String json = OPENxtraceUtils.serializeTraceListToJsonString(traces);

		return ResponseEntity.ok(json);
	}

	@RequestMapping(value = PUSH_LINK, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushDataViaLink(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody MeasurementDataSpec spec)
			throws IOException {
		if (ResourceUtils.isUrl(spec.getLink())) {
			return ResponseEntity.badRequest().body("Improperly formatted link: " + spec.getLink());
		}

		LOGGER.info("Received link to {} for {}@{}.", spec.getType().toPrettyString(), aid, version);

		switch (spec.getType()) {
		case ACCESS_LOGS:
			String accessLogs = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushAccessLogs(aid, version, accessLogs);
		case OPEN_XTRACE:
			String tracesAsJson = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushOpenXtraces(aid, version, tracesAsJson);
		case CSV:
			String csvContent = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushCsv(aid, version, csvContent);
		case INSPECTIT:
		default:
			return ResponseEntity.badRequest().body("Unsupported measurement data type: " + spec.getType().toPrettyString());
		}
	}

	@RequestMapping(value = PUSH_OPEN_XTRACE, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushOpenXtraces(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String tracesAsJson)
			throws IOException {
		LOGGER.info("Received OPEN.xtraces for {}@{}.", aid, version);
		return storeTraces(aid, version, tracesAsJson, null, null);
	}

	@RequestMapping(value = PUSH_ACCESS_LOGS, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushAccessLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String accessLogs)
			throws IOException {
		LOGGER.info("Received access logs for {}@{}.", aid, version);

		List<AccessLogEntry> parsedLogs = new ArrayList<>();

		for (String line : accessLogs.split("\\n")) {
			parsedLogs.add(AccessLogEntry.fromLogLine(line));
		}

		List<Trace> traces = new AccessLogsToOpenXtraceConverter(configProvider.getOrDefault(aid).isHashSessionId()).convert(parsedLogs);
		return storeTraces(aid, version, traces);
	}

	@RequestMapping(value = PUSH_CSV, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushCsv(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String csvContent)
			throws IOException {
		LOGGER.info("Received CSV for {}@{}.", aid, version);

		List<CsvRow> csvRows = CsvRow.listFromString(csvContent);

		List<Trace> traces = new CsvRowToOpenXtraceConverter(configProvider.getOrDefault(aid).isHashSessionId()).convert(csvRows);
		return storeTraces(aid, version, traces);
	}

	private ResponseEntity<String> storeTraces(AppId aid, VersionOrTimestamp version, List<Trace> traces) throws IOException {
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

		String tracesAsJson = OPENxtraceUtils.serializeTraceListToJsonString(traces);

		return storeTraces(aid, version, tracesAsJson, from, to);
	}

	private ResponseEntity<String> storeTraces(AppId aid, VersionOrTimestamp version, String tracesAsJson, Date from, Date to) {
		amqpTemplate.convertAndSend(AmqpApi.SessionLogs.TASK_PROCESS_TRACES.name(), AmqpApi.SessionLogs.TASK_PROCESS_TRACES.formatRoutingKey().of(aid, version), tracesAsJson);

		LOGGER.info("{}@{} Forwarded traces to AMQP handler.", aid, version);

		String link = RestApi.SessionLogs.MeasurementData.GET_VERSION.requestUrl(aid, version).withQueryIfNotEmpty("from", formatOrNull(from)).withQueryIfNotEmpty("to", formatOrNull(to))
				.withoutProtocol().get();

		return ResponseEntity.accepted().body(link);
	}

	private String formatOrNull(Date date) {
		if (date == null) {
			return null;
		} else {
			return ApiFormats.DATE_FORMAT.format(date);
		}
	}

}
