package org.continuity.frontend.controllers;

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
@RequestMapping("loadtest")
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
	@RequestMapping(path = "{type}/createandexecute", method = RequestMethod.POST)
	public ResponseEntity<String> createAndExecuteLoadTest(@PathVariable("type") String testType, @RequestBody LoadTestSpecification specification) {
		String message;
		HttpStatus status;

		if (specification == null) {
			message = "Load test specification is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getWorkloadModelType() == null) || "".equals(specification.getWorkloadModelType())) {
			message = "Workload model type is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getWorkloadModelId() == null) || "".equals(specification.getWorkloadModelId())) {
			message = "Workload model ID is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getTag() == null) || "".equals(specification.getTag())) {
			message = "Tag is required.";
			status = HttpStatus.BAD_REQUEST;
		} else {
			amqpTemplate.convertAndSend(RabbitMqConfig.CREATE_AND_EXECUTE_LOAD_TEST_EXCHANGE_NAME, specification.getWorkloadModelType() + "." + testType, specification);
			message = "Creating a load test from" + specification.getWorkloadModelType() + " workload model " + specification.getWorkloadModelId();
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
	@RequestMapping(path = "{type}/execute", method = RequestMethod.POST)
	public ResponseEntity<String> executeLoadTest(@PathVariable("type") String testType, @RequestBody JsonNode testPlan) {
		String message;
		HttpStatus status;

		if (testPlan == null) {
			message = "Load test is required.";
			status = HttpStatus.BAD_REQUEST;
		} else {
			amqpTemplate.convertAndSend(RabbitMqConfig.EXECUTE_LOAD_TEST_EXCHANGE_NAME, testType, testPlan);
			message = "Executing a " + testType + " load test";
			status = HttpStatus.ACCEPTED;
		}

		return new ResponseEntity<>(message, status);
	}

	/**
	 * Creates a new load test.
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
	@RequestMapping(value = "{lt-type}/{wm-type}/{id}/create", method = RequestMethod.GET)
	public ResponseEntity<JsonNode> createAndGetLoadTest(@PathVariable("lt-type") String loadTestType, @PathVariable("wm-type") String workloadModelType, @PathVariable("id") String workloadModelId,
			@RequestParam String tag) {
		LOGGER.debug("load test type: {}, workload model type: {}, workload model id: {}, tag: {}", loadTestType, workloadModelType, workloadModelId, tag);
		return restTemplate.getForEntity("http://" + loadTestType + "/loadtest/" + workloadModelType + "/" + workloadModelId + "/create?tag=" + tag, JsonNode.class);
	}

	/**
	 * Get Report of Loadtest.
	 * 
	 * @param timeout
	 *            the time in millis, how long should be waited for the report.
	 * @return
	 */
	@RequestMapping(value = "/report", method = RequestMethod.GET)
	public ResponseEntity<String> getReportOfLoadtest(@RequestParam(value = "timeout", required = true) long timeout) {
		return new ResponseEntity<String>(amqpTemplate.receiveAndConvert(RabbitMqConfig.PROVIDE_REPORT_QUEUE_NAME, timeout, new ParameterizedTypeReference<String>() {
		}), HttpStatus.OK);

	}
}
