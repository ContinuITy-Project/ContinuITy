package org.continuity.session.logs.controllers;

import static org.continuity.api.rest.RestApi.SessionLogs.Paths.GET;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.continuity.session.logs.managers.SessionLogsPipelineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Alper Hi
 *
 */
@RestController
public class SessionLogsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsController.class);

	@Autowired
	@Qualifier("plainRestTemplate")
	private RestTemplate plainRestTemplate;

	@Autowired
	private RestTemplate eurekaRestTemplate;

	/**
	 * @param link
	 * @return
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<String> getSessionLogsFromLink(@RequestParam String link, @RequestParam(required = false) String tag) {
		try {
			link = URLDecoder.decode(link, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Could not decode URL!", e);
		}

		LOGGER.info("Creating session logs for tag {} from data {}", tag, link);

		try {
			new URL(link);
		} catch (MalformedURLException e) {
			LOGGER.error("Received malformed URL!", e);
			return ResponseEntity.badRequest().body("Malformed URL: " + link);
		}

		SessionLogsPipelineManager manager = new SessionLogsPipelineManager(link, tag, plainRestTemplate, eurekaRestTemplate);

		String sessionLog = manager.runPipeline();

		LOGGER.info("Session logs created for tag {} from data {}", tag, link);
		return ResponseEntity.ok(sessionLog);
	}
}
