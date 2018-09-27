package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Loadtest.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Loadtest.Paths.DELETE_REPORT;
import static org.continuity.api.rest.RestApi.Orchestrator.Loadtest.Paths.GET;
import static org.continuity.api.rest.RestApi.Orchestrator.Loadtest.Paths.POST;
import static org.continuity.api.rest.RestApi.Orchestrator.Loadtest.Paths.REPORT;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping(ROOT)
public class LoadTestController {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoadTestController.class);

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Retrieves the created load test of the specified type and id. It does not wait if the test is
	 * not yet created.
	 *
	 * @param type
	 *            The type of the load test (e.g., jmeter).
	 * @param id
	 *            The ID of the load test.
	 * @return The load test.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<JsonNode> getLoadTest(@PathVariable String type, @PathVariable String id) {
		String link = RestApi.Generic.GET_LOAD_TEST.get(type).requestUrl(id).get();

		LOGGER.info("Trying to get the load test from {}", link);

		return restTemplate.getForEntity(link, JsonNode.class);
	}

	/**
	 * Retrieves the created load test report of the specified type and id. It does not wait if the
	 * report is not yet created.
	 *
	 * @param type
	 *            The type of the load test (e.g., jmeter).
	 * @param id
	 *            The ID of the load test report.
	 * @return The load test report.
	 */
	@RequestMapping(path = REPORT, method = RequestMethod.GET)
	public ResponseEntity<String> getReport(@PathVariable String type, @PathVariable String id) {
		String link = RestApi.Generic.GET_LOAD_TEST_REPORT.get(type).requestUrl(id).get();

		LOGGER.info("Trying to get the report from {}", link);

		return restTemplate.getForEntity(link, String.class);
	}

	/**
	 * Deletes the created load test report of the specified type and id. It does not wait if the
	 * report is not yet created.
	 *
	 * @param type
	 *            The type of the load test (e.g., jmeter).
	 * @param id
	 *            The ID of the load test report.
	 */
	@RequestMapping(path = DELETE_REPORT, method = RequestMethod.DELETE)
	public void deleteReport(@PathVariable String type, @PathVariable String id) {
		String link = RestApi.Generic.DELETE_LOAD_TEST_REPORT.get(type).requestUrl(id).get();

		LOGGER.info("Trying to delete the report at {}", link);

		restTemplate.delete(link);
	}

	/**
	 * Uploads a test configuration and returns the corresponding link
	 *
	 * @param type
	 *            The type of the load test (e.g., jmeter).
	 * @param testConfiguration
	 *            the testConfiguration
	 * @param tag
	 *            the corresponding tag
	 * @return Returns a {@link LinkExchangeModel} containing a link to the test configuration
	 */
	@RequestMapping(path = POST, method = RequestMethod.POST)
	public ResponseEntity<LinkExchangeModel> uploadTestPlan(@PathVariable String type, @RequestBody ObjectNode testConfiguration, @PathVariable String tag) {
		String link = RestApi.Generic.UPLOAD_LOAD_TEST.get(type).requestUrl(tag).get();

		LOGGER.info("Trying to upload the test configuration at {}", link);

		return restTemplate.postForEntity(link, testConfiguration, LinkExchangeModel.class);
	}
	
	

}
