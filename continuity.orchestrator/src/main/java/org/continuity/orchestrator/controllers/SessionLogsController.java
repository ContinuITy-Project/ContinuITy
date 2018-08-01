package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.SessionLogs.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.SessionLogs.Paths.GET;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.rest.RestApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping(ROOT)
public class SessionLogsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsController.class);

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Retrieves the created session logs of the specified id. It does not wait if the logs are not
	 * yet created.
	 *
	 * @param id
	 *            The ID of the session logs.
	 * @return The session logs.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<SessionLogs> getSessionLogs(@PathVariable String id) {
		LOGGER.info("Trying to get the session logs from {}", RestApi.SessionLogs.GET.requestUrl(id).get());
		return restTemplate.getForEntity(RestApi.SessionLogs.GET.requestUrl(id).get(), SessionLogs.class);
	}

}
