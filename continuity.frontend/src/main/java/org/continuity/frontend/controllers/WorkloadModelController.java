package org.continuity.frontend.controllers;

import java.util.Collections;

import org.continuity.frontend.config.RabbitMqConfig;
import org.continuity.frontend.entities.ModelCreatedReport;
import org.continuity.frontend.entities.WorkloadModelConfig;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
	@RequestMapping(path = "/create", method = RequestMethod.POST)
	public ResponseEntity<ModelCreatedReport> createWorkloadModel(@RequestBody WorkloadModelConfig config) {
		ModelCreatedReport report;
		HttpStatus status;

		if (config == null) {
			report = new ModelCreatedReport("Workload model configuration is required. Format:\n{\n\t\"type\": \"workload model type\",\n\t\"link\": \"link to monitoring data\"\n}");
			status = HttpStatus.BAD_REQUEST;
		} else if ((config.getWorkloadModelType() == null) || "".equals(config.getMonitoringDataLink())) {
			report = new ModelCreatedReport("Need to specify the workload model type. Request bod format:\n{\n\t\"type\": \"workload model type\",\n\t\"link\": \"link to monitoring data\"\n}");
			status = HttpStatus.BAD_REQUEST;
		} else if ((config.getMonitoringDataLink() == null) || "".equals(config.getMonitoringDataLink())) {
			report = new ModelCreatedReport("Need to specify a link to monitoring data. Request bod format:\n{\n\t\"type\": \"workload model type\",\n\t\"link\": \"link to monitoring data\"\n}");
			status = HttpStatus.BAD_REQUEST;
		} else {
			Object response = amqpTemplate.convertSendAndReceive(RabbitMqConfig.MONITORING_DATA_AVAILABLE_EXCHANGE_NAME, config.getWorkloadModelType(), config);
			report = new ModelCreatedReport("Creating a " + config.getWorkloadModelType() + " model.", response.toString());
			status = HttpStatus.ACCEPTED;
		}

		return new ResponseEntity<>(report, status);
	}

	@RequestMapping(path = "/get", method = RequestMethod.GET)
	public ResponseEntity<JsonNode> getWorkloadModel(@RequestBody String link) {
		return restTemplate.getForEntity(addProtocolIfMissing(link + "/workload"), JsonNode.class, Collections.emptyMap());
	}

	private String addProtocolIfMissing(String url) {
		if (url.startsWith("http")) {
			return url;
		} else {
			return "http://" + url;
		}
	}

}
