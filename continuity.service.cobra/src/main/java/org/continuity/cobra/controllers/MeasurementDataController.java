package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.GET;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.GET_VERSION;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.PUSH_ACCESS_LOGS;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.PUSH_CSV;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.PUSH_LINK;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.PUSH_OPEN_XTRACE;
import static org.continuity.api.rest.RestApi.Cobra.MeasurementData.Paths.PUSH_SESSION_LOGS;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.amqp.RoutingKeyFormatter.AppIdAndVersion;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.config.MeasurementDataSpec;
import org.continuity.api.rest.RestApi;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
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

	@RequestMapping(value = GET, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getTraces(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam(required = false) List<String> from, @RequestParam(required = false) List<String> to)
			throws IOException, TimeoutException {

		return getTracesForVersion(aid, null, from, to);
	}

	@RequestMapping(value = GET_VERSION, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getTracesForVersion(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version,
			@RequestParam(required = false) List<String> from, @RequestParam(required = false) List<String> to) throws IOException, TimeoutException {

		if (((from == null) && (to != null)) || ((from != null) && (to == null)) || ((from != null) && (to != null) && (from.size() != to.size()))) {
			return ResponseEntity.badRequest().body("'from' and 'to' need to have same length!");
		}

		List<Trace> traces;

		if ((from == null) && (to == null)) {
			traces = manager.readTraces(aid, version, null, null);
		} else {
			Iterator<String> fromIter = from.iterator();
			Iterator<String> toIter = to.iterator();

			traces = new ArrayList<>();

			while (fromIter.hasNext() && toIter.hasNext()) {
				String f = fromIter.next();
				String t = toIter.next();

				Date dFrom = null;

				try {
					dFrom = ApiFormats.DATE_FORMAT.parse(f);
				} catch (ParseException e) {
					LOGGER.error("Cannot parse from date!", e);
					return ResponseEntity.badRequest().body("Illegal date format of 'from' date: " + f);
				}

				Date dTo = null;

				try {
					dTo = ApiFormats.DATE_FORMAT.parse(t);
				} catch (ParseException e) {
					LOGGER.error("Cannot parse to date!", e);
					return ResponseEntity.badRequest().body("Illegal date format of 'to' date: " + t);
				}

				traces.addAll(manager.readTraces(aid, version, dFrom, dTo));
			}
		}

		String json = OPENxtraceUtils.serializeTraceListToJsonString(traces);

		return ResponseEntity.ok(json);
	}

	@RequestMapping(value = PUSH_LINK, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushDataViaLink(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody MeasurementDataSpec spec,
			@RequestParam(defaultValue = "false") boolean finish)
			throws IOException {
		if (ResourceUtils.isUrl(spec.getLink())) {
			return ResponseEntity.badRequest().body("Improperly formatted link: " + spec.getLink());
		}

		LOGGER.info("Received link to {} for {}@{}.", spec.getType().toPrettyString(), aid, version);

		switch (spec.getType()) {
		case ACCESS_LOGS:
			String accessLogs = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushAccessLogs(aid, version, accessLogs, finish);
		case OPEN_XTRACE:
			String tracesAsJson = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushOpenXtraces(aid, version, tracesAsJson, finish);
		case CSV:
			String csvContent = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushCsv(aid, version, csvContent, finish);
		case SESSION_LOGS:
			String sessionContent = plainRestTemplate.getForObject(spec.getLink(), String.class);
			return pushSessionLogs(aid, version, sessionContent, finish);
		case INSPECTIT:
		default:
			return ResponseEntity.badRequest().body("Unsupported measurement data type: " + spec.getType().toPrettyString());
		}
	}

	@RequestMapping(value = PUSH_OPEN_XTRACE, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushOpenXtraces(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String tracesAsJson,
			@RequestParam(defaultValue = "false") boolean finish)
			throws IOException {
		LOGGER.info("Received OPEN.xtraces for {}@{}.", aid, version);

		return forwardData(AmqpApi.Cobra.TASK_PROCESS_TRACES, aid, version, tracesAsJson, finish);
	}

	@RequestMapping(value = PUSH_ACCESS_LOGS, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushAccessLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String accessLogs,
			@RequestParam(defaultValue = "false") boolean finish)
			throws IOException {
		LOGGER.info("Received access logs for {}@{}.", aid, version);

		return forwardData(AmqpApi.Cobra.TASK_TRANSFORM_ACCESSLOGS, aid, version, accessLogs, finish);
	}

	@RequestMapping(value = PUSH_CSV, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushCsv(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String csvContent,
			@RequestParam(defaultValue = "false") boolean finish)
			throws IOException {
		LOGGER.info("Received CSV for {}@{}.", aid, version);

		return forwardData(AmqpApi.Cobra.TASK_TRANSFORM_CSV, aid, version, csvContent, finish);
	}

	@RequestMapping(value = PUSH_SESSION_LOGS, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> pushSessionLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable("version") VersionOrTimestamp version, @RequestBody String sessionContent,
			@RequestParam(defaultValue = "false") boolean finish) throws IOException {
		LOGGER.info("Received session logs for {}@{}.", aid, version);

		return forwardData(AmqpApi.Cobra.TASK_TRANSFORM_SESSIONLOGS, aid, version, sessionContent, finish);
	}

	private ResponseEntity<String> forwardData(ExchangeDefinition<AppIdAndVersion> exchange, AppId aid, VersionOrTimestamp version, String data, boolean finish) {
		amqpTemplate.convertAndSend(exchange.name(), exchange.formatRoutingKey().of(aid, version), data, AmqpApi.Cobra.finishHeader(finish));

		LOGGER.info("{}@{} Forwarded data to {}.", aid, version, exchange.name());

		String link = RestApi.Cobra.MeasurementData.GET_VERSION.requestUrl(aid, version).withoutProtocol().get();

		return ResponseEntity.accepted().body(link);
	}

}
