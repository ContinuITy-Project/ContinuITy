package org.continuity.frontend.controllers;

import org.continuity.frontend.config.RabbitMqConfig;
import org.continuity.frontend.entities.LoadTestSpecification;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controls load testing.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("loadtest")
public class LoadTestController {

	@Autowired
	private AmqpTemplate amqpTemplate;

	/**
	 * Causes creation of a new load test.
	 *
	 * @param specification
	 *            The specification of the load test.
	 * @return A report.
	 */
	@RequestMapping(path = "{type}/create", method = RequestMethod.POST)
	public ResponseEntity<String> createLoadTest(@PathVariable("type") String testType, @RequestBody LoadTestSpecification specification) {
		String message;
		HttpStatus status;

		if (specification == null) {
			message = "Load test specification is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getWorkloadModelType() == null) || "".equals(specification.getWorkloadModelLink())) {
			message = "Workload model type is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getWorkloadModelLink() == null) || "".equals(specification.getWorkloadModelLink())) {
			message = "Workload model link is required.";
			status = HttpStatus.BAD_REQUEST;
		} else if ((specification.getTag() == null) || "".equals(specification.getTag())) {
			message = "Tag is required.";
			status = HttpStatus.BAD_REQUEST;
		} else {
			amqpTemplate.convertAndSend(RabbitMqConfig.LOAD_TEST_NEEDED_EXCHANGE_NAME, specification.getWorkloadModelType() + "." + testType, specification);
			message = "Creating a load test from" + specification.getWorkloadModelType() + " workload model " + specification.getWorkloadModelLink();
			status = HttpStatus.ACCEPTED;
		}

		return new ResponseEntity<>(message, status);
	}

}
