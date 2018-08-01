package org.continuity.session.logs.controllers;

import static org.continuity.api.rest.RestApi.SessionLogs.Paths.GET;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Alper Hi
 * @author Henning Schulz
 *
 */
@RestController()
@RequestMapping(RestApi.SessionLogs.ROOT)
public class SessionLogsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsController.class);

	@Autowired
	private MemoryStorage<SessionLogs> storage;

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
}
