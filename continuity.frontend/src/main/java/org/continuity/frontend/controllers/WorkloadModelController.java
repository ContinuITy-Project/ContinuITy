package org.continuity.frontend.controllers;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.continuity.frontend.config.RabbitMqConfig;
import org.continuity.frontend.entities.ModelCreatedReport;
import org.continuity.frontend.entities.WorkloadModelConfig;
import org.continuity.frontend.entities.WorkloadModelInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.rabbitmq.client.Channel;

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

	private static final String RESPONSE_QUEUE_PREFIX = "continuity.frontend.workloadmodel.created.";

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ConnectionFactory connectionFactory;

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
			ResponseEntity<String> linkResponse = restTemplate.getForEntity("http://" + type + "/model/" + config.getTag() + "/reserve", String.class);

			if (!linkResponse.getStatusCode().is2xxSuccessful()) {
				status = linkResponse.getStatusCode();
				report = new ModelCreatedReport("Problem during reserving workload model link: " + linkResponse.getBody());
			} else {
				declareResponseQueue(linkResponse.getBody());

				WorkloadModelInput input = new WorkloadModelInput(config.getMonitoringDataLink(), linkResponse.getBody());
				amqpTemplate.convertAndSend(RabbitMqConfig.MONITORING_DATA_AVAILABLE_EXCHANGE_NAME, type, input);

				report = new ModelCreatedReport("Creating a " + type + " model.", linkResponse.getBody());
				status = HttpStatus.ACCEPTED;
			}
		}

		return new ResponseEntity<>(report, status);
	}

	private void declareResponseQueue(String workloadLink) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = RESPONSE_QUEUE_PREFIX + workloadLink;
			channel.queueDeclare(queueName, false, false, false, Collections.emptyMap());
			channel.queueBind(queueName, RabbitMqConfig.WORKLOAD_MODEL_CREATED_EXCHANGE_NAME, "*." + workloadLink);

			LOGGER.info("Declared a response queue for {}.", workloadLink);
		} catch (IOException e) {
			LOGGER.error("Could not create a response queue for {}.", workloadLink);
			e.printStackTrace();
		}
	}

	private void deleteResponseQueue(String workloadLink) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = RESPONSE_QUEUE_PREFIX + workloadLink;
			channel.queueDelete(queueName);

			LOGGER.info("Deleted the response queue for {}.", workloadLink);
		} catch (IOException e) {
			LOGGER.error("Could not delete the response queue for {}.", workloadLink);
			e.printStackTrace();
		}
	}

	/**
	 * Waits for the corresponding workload model service to finish creation of the model.
	 *
	 * @param request
	 *            The request.
	 * @param timeout
	 *            A timeout for stopping waiting.
	 * @return The workload model
	 */
	@RequestMapping(path = "wait/**", method = RequestMethod.GET)
	public ResponseEntity<JsonNode> waitForModelCreatedUnencoded(HttpServletRequest request, @RequestParam long timeout) {
		String link = extractWorkloadLink(request);
		LOGGER.info("Waiting for the workload model at {} to be created", link);

		JsonNode response;
		try {
			response = amqpTemplate.receiveAndConvert(RESPONSE_QUEUE_PREFIX + link, timeout, ParameterizedTypeReference.forType(JsonNode.class));
		} catch (AmqpIOException e) {
			LOGGER.error("Cannot wait for not existing response queue of model {}", link);

			Throwable t = e;
			while (t.getCause() != null) {
				t = t.getCause();
			}
			LOGGER.error("Error message from {}: {}", t.getClass().getSimpleName(), t.getMessage());

			return ResponseEntity.badRequest().body(new TextNode("Cannot wait for " + link));
		}

		if (response != null) {
			boolean error = false;
			if (response.has("error")) {
				error = Boolean.parseBoolean(response.get("error").asText());
			}

			deleteResponseQueue(link);

			if (!error) {
				LOGGER.info("Workload model {} is ready.", link);
				return ResponseEntity.ok(response);
			} else {
				LOGGER.error("Error during creation of workload model {}!", link);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
			}
		} else {
			LOGGER.info("Workload model {} was not ready, yet.", link);

			return ResponseEntity.noContent().build();
		}
	}

	/**
	 * Retrieves the created workload model of the specified type and id. It does not wait if the
	 * model is not yet created.
	 *
	 * @param request
	 *            The request.
	 * @return The workload model.
	 */
	@RequestMapping(path = "get/**", method = RequestMethod.GET)
	public ResponseEntity<JsonNode> getWorkloadModelUnencoded(HttpServletRequest request) {
		String link = extractWorkloadLink(request);
		LOGGER.info("Trying to get the workload model from {}", link);
		return restTemplate.getForEntity("http://" + link, JsonNode.class);
	}

	private String extractWorkloadLink(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

		AntPathMatcher apm = new AntPathMatcher();
		return apm.extractPathWithinPattern(bestMatchPattern, path);
	}

}
