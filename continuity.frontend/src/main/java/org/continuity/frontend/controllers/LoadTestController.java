package org.continuity.frontend.controllers;

import static org.continuity.api.rest.RestApi.Frontend.Loadtest.ROOT;
import static org.continuity.api.rest.RestApi.Frontend.Loadtest.Paths.CREATE_AND_EXECUTE;
import static org.continuity.api.rest.RestApi.Frontend.Loadtest.Paths.CREATE_AND_GET;
import static org.continuity.api.rest.RestApi.Frontend.Loadtest.Paths.EXECUTE;
import static org.continuity.api.rest.RestApi.Frontend.Loadtest.Paths.REPORT_PATH;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.rest.RestApi.Generic;
import org.continuity.frontend.config.RabbitMqConfig;
import org.continuity.frontend.entities.LoadTestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Controls load testing.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class LoadTestController {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoadTestController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	/**
	 * Causes creation from a workload model and execution of a load test.
	 *
	 * @param testType
	 *            The type of the load test, e.g., BenchFlow.
	 * @param specification
	 *            The specification of the load test.
	 * @return A report.
	 */
	@RequestMapping(path = CREATE_AND_EXECUTE, method = RequestMethod.POST)
	public ResponseEntity<String> createAndExecuteLoadTest(@PathVariable("type") String testType, @RequestBody LoadTestSpecification specification) {
		String message;
		HttpStatus status;

		if (specification == null) {
			message = "Load test specification is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getWorkloadModelLink() == null) || "".equals(specification.getWorkloadModelLink())) {
			message = "Workload model link is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getTag() == null) || "".equals(specification.getTag())) {
			message = "Tag is required.";
			status = HttpStatus.BAD_REQUEST;
		} else {
			String workloadType = extractWorkloadType(specification.getWorkloadModelLink());

			amqpTemplate.convertAndSend(AmqpApi.Frontend.LOADTESTCREATIONANDEXECUTION_REQUIRED.name(),
					AmqpApi.Frontend.LOADTESTCREATIONANDEXECUTION_REQUIRED.formatRoutingKey().of(workloadType, testType), specification);
			message = "Creating a load test from" + workloadType + " workload model " + specification.getWorkloadModelLink();
			status = HttpStatus.ACCEPTED;
		}

		return new ResponseEntity<>(message, status);
	}

	/**
	 * Causes execution of a load test.
	 *
	 * @param testType
	 *            The type of the load test, e.g., BenchFlow.
	 * @param testPlan
	 *            The load test to be executed.
	 * @return A report.
	 */
	@RequestMapping(path = EXECUTE, method = RequestMethod.POST)
	public ResponseEntity<String> executeLoadTest(@PathVariable("type") String testType, @RequestBody JsonNode testPlan) {
		String message;
		HttpStatus status;

		if (testPlan == null) {
			message = "Load test is required.";
			status = HttpStatus.BAD_REQUEST;
		} else {
			amqpTemplate.convertAndSend(AmqpApi.Frontend.LOADTESTEXECUTION_REQUIRED.name(), AmqpApi.Frontend.LOADTESTEXECUTION_REQUIRED.formatRoutingKey().of(testType), testPlan);
			message = "Executing a " + testType + " load test";
			status = HttpStatus.ACCEPTED;
		}

		return new ResponseEntity<>(message, status);
	}

	/**
	 * Creates a new load test from a workload model. The workload model is specified by a
	 * decomposed link: {@code workloadModelType/model/workloadModelId}
	 *
	 * @param loadTestType
	 *            The type of the load test.
	 * @param workloadModelType
	 *            The type of the workload model.
	 * @param workloadModelId
	 *            The id of the workload model.
	 * @param tag
	 *            The tag of the annotation to be used.
	 * @return The load test.
	 */
	@RequestMapping(value = CREATE_AND_GET, method = RequestMethod.GET)
	public ResponseEntity<JsonNode> createAndGetLoadTest(@PathVariable("lt-type") String loadTestType, @PathVariable("wm-type") String workloadModelType, @PathVariable("id") String workloadModelId,
			@RequestParam String tag) {
		LOGGER.debug("load test type: {}, workload model type: {}, workload model id: {}, tag: {}", loadTestType, workloadModelType, workloadModelId, tag);
		return restTemplate.getForEntity(Generic.GET_AND_CREATE_LOAD_TEST.get(loadTestType).requestUrl(workloadModelType, workloadModelId).withQuery("tag", tag).get(), JsonNode.class);
	}

	private String extractWorkloadType(String workloadLink) {
		return workloadLink.split("/")[0];
	}

	/**
	 * Get Report of Loadtest.
	 *
	 * @param timeout
	 *            the time in millis, how long should be waited for the report.
	 * @return
	 */
	@RequestMapping(value = REPORT_PATH, method = RequestMethod.GET)
	public ResponseEntity<String> getReportOfLoadtest(@RequestParam(value = "timeout", required = true) long timeout) {
		return new ResponseEntity<String>(amqpTemplate.receiveAndConvert(RabbitMqConfig.LOAD_TEST_REPORT_AVAILABLE_QUEUE_NAME, timeout, new ParameterizedTypeReference<String>() {
		}), HttpStatus.OK);

	}
}
