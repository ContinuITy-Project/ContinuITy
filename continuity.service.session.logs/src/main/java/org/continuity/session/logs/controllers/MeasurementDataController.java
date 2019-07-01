package org.continuity.session.logs.controllers;

import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.ROOT;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.GET;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.GET_VERSION;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_ACCESS_LOGS;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_CSV;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_LINK;
import static org.continuity.api.rest.RestApi.SessionLogs.MeasurementData.Paths.PUSH_OPEN_XTRACE;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.TimeoutException;

import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.config.MeasurementDataSpec;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.session.logs.converter.AccessLogsToOpenXtraceConverter;
import org.continuity.session.logs.converter.CsvRowToOpenXtraceConverter;
import org.continuity.session.logs.entities.CsvRow;
import org.continuity.session.logs.managers.ElasticsearchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
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
	private ElasticsearchManager manager;

	private final AccessLogsToOpenXtraceConverter accessLogsConverter = new AccessLogsToOpenXtraceConverter();

	private final CsvRowToOpenXtraceConverter csvConverter = new CsvRowToOpenXtraceConverter();

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
		List<Trace> traces = OPENxtraceUtils.deserializeIntoTraceList(tracesAsJson);
		return storeTraces(aid, version, traces);
	}

	@RequestMapping(value = PUSH_ACCESS_LOGS, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushAccessLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String accessLogs)
			throws IOException {
		List<AccessLogEntry> parsedLogs = new ArrayList<>();

		for (String line : accessLogs.split("\\n")) {
			parsedLogs.add(AccessLogEntry.fromLogLine(line));
		}

		List<Trace> traces = accessLogsConverter.convert(parsedLogs);
		return storeTraces(aid, version, traces);
	}

	@RequestMapping(value = PUSH_CSV, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushCsv(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String csvContent)
			throws IOException {
		List<CsvRow> csvRows = CsvRow.listFromString(csvContent);

		List<Trace> traces = csvConverter.convert(csvRows);
		return storeTraces(aid, version, traces);
	}

	private ResponseEntity<String> storeTraces(AppId aid, VersionOrTimestamp version, List<Trace> traces) throws IOException {
		manager.storeTraces(aid, version, traces);

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

		String link = RestApi.SessionLogs.MeasurementData.GET_VERSION.requestUrl(aid, version).withQuery("from", ApiFormats.DATE_FORMAT.format(from)).withQuery("to", ApiFormats.DATE_FORMAT.format(to))
				.withoutProtocol().get();

		return ResponseEntity.created(URI.create("http://" + link)).body(link);
	}

}
