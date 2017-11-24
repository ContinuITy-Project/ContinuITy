package org.continuity.frontend.controllers;

import javax.servlet.http.HttpServletRequest;

import org.continuity.frontend.config.RabbitMqConfig;
import org.continuity.frontend.entities.ModelCreatedReport;
import org.continuity.frontend.entities.WorkloadModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Controls workload models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("/workloadmodel")
public class WorkloadModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadModelController.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Causes creation of a new workload model of the specified type and from the specified data.
	 *
	 * @param config
	 *            Configuration holding the type and data.
	 * @return A report.
	 */
	@RequestMapping(path = "{type}/create", method = RequestMethod.POST)
	public ResponseEntity<ModelCreatedReport> createWorkloadModel(@PathVariable("type") String type, @RequestBody WorkloadModelConfig config) {
		ModelCreatedReport report;
		HttpStatus status;

		if (config == null) {
			report = new ModelCreatedReport("Workload model configuration is required.");
			status = HttpStatus.BAD_REQUEST;
		} else if ((config.getMonitoringDataLink() == null) || "".equals(config.getMonitoringDataLink())) {
			report = new ModelCreatedReport("Need to specify a link to monitoring data.");
			status = HttpStatus.BAD_REQUEST;
		} else {
			Object response = amqpTemplate.convertSendAndReceive(RabbitMqConfig.MONITORING_DATA_AVAILABLE_EXCHANGE_NAME, type, config);

			if (response == null) {
				report = new ModelCreatedReport("Could not create a workload model of type " + type + "! There was no appropriate handler.");
				status = HttpStatus.BAD_REQUEST;
			} else {
				report = new ModelCreatedReport("Creating a " + type + " model.", response.toString());
				status = HttpStatus.ACCEPTED;
			}
		}

		return new ResponseEntity<>(report, status);
	}

	/**
	 * Retrieves the created workload model of the specified type and id.
	 *
	 * @param request
	 *            The request.
	 * @return The workload model.
	 */
	@RequestMapping(path = "get/**", method = RequestMethod.GET)
	public ResponseEntity<JsonNode> getWorkloadModelUnencoded(HttpServletRequest request) {
		String link = extractWorkloadLink(request);
		LOGGER.info("Trying to get the workload model from {}", link);
		return restTemplate.getForEntity(link, JsonNode.class);
	}

	private String extractWorkloadLink(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

		AntPathMatcher apm = new AntPathMatcher();
		return "http://" + apm.extractPathWithinPattern(bestMatchPattern, path);
	}

}
