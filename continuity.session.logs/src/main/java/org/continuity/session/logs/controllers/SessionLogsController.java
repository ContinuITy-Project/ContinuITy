package org.continuity.session.logs.controllers;

import static org.continuity.api.rest.RestApi.SessionLogs.Paths.CREATE;
import static org.continuity.api.rest.RestApi.SessionLogs.Paths.GET;

import java.util.List;

import org.continuity.api.entities.artifact.ModularizedSessionLogs;
import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.artifact.SessionLogsInput;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.session.logs.extractor.ModularizedOPENxtraceSessionLogsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import open.xtrace.OPENxtraceUtils;

/**
 *
 * @author Alper Hi
 * @author Henning Schulz
 *
 */
@RestController()
@RequestMapping(RestApi.SessionLogs.ROOT)
public class SessionLogsController {

	@Autowired
	private RestTemplate eurekaRestTemplate;

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsController.class);

	@Autowired
	private MixedStorage<SessionLogs> storage;

	/**
	 * Provides the already generated session logs with the provided id.
	 * 
	 * @param id
	 *            the id of the session logs.
	 * @return {@link SessionLogs}
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<SessionLogs> getSessionLogsFromLink(@PathVariable String id) {
		SessionLogs sessionLogs = storage.get(id);

		if (sessionLogs == null) {
			LOGGER.warn("Could not find session logs for id {}!", id);
			return ResponseEntity.notFound().build();
		} else {
			LOGGER.info("Returned session logs for id {}!", id);
			return ResponseEntity.ok(sessionLogs);
		}
	}

	/**
	 * Creates session logs based on the provided input data. The Session logs will be directly
	 * passed and are not stored in the storage.
	 * 
	 * @param sessionLogsInput
	 *            Provides the traces and the target services.
	 * @return {@link SessionLogs}
	 */
	@RequestMapping(value = CREATE, method = RequestMethod.POST)
	public ResponseEntity<ModularizedSessionLogs> getModularizedSessionLogs(@RequestBody SessionLogsInput sessionLogsInput) {
		ModularizedOPENxtraceSessionLogsExtractor extractor = new ModularizedOPENxtraceSessionLogsExtractor("", eurekaRestTemplate, sessionLogsInput.getServices());
		List<Trace> traces = OPENxtraceUtils.deserializeIntoTraceList(sessionLogsInput.getSerializedTraces());
		ModularizedSessionLogs sessionLogsResult = extractor.getSessionLogsAndThinkTimes(traces);

		return ResponseEntity.ok(sessionLogsResult);
	}
}
