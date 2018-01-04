package org.continuity.workload.annotation.amqp;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.workload.annotation.config.RabbitMqConfig;
import org.continuity.workload.annotation.entities.AnnotationValidityReport;
import org.continuity.workload.annotation.entities.WorkloadModelLink;
import org.continuity.workload.annotation.storage.AnnotationStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@Component
public class AnnotationAmpqHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationAmpqHandler.class);

	private final AnnotationStorageManager storageManager;

	private final RestTemplate restTemplate;

	private final AmqpTemplate amqpTemplate;

	@Autowired
	public AnnotationAmpqHandler(AnnotationStorageManager storageManager, RestTemplate restTemplate, AmqpTemplate amqpTemplate) {
		this.storageManager = storageManager;
		this.restTemplate = restTemplate;
		this.amqpTemplate = amqpTemplate;
	}

	@RabbitListener(queues = RabbitMqConfig.MODEL_CREATED_QUEUE_NAME)
	public void onModelCreated(WorkloadModelLink link) {
		LOGGER.info("Received workload model link: {}", link);

		ResponseEntity<SystemModel> systemResponse = restTemplate.getForEntity(addProtocolIfMissing(link.getSystemModelLink()), SystemModel.class);
		if (systemResponse.getStatusCode() != HttpStatus.OK) {
			LOGGER.error("Could not retrieve system model from {}. Got response code {}!", link.getSystemModelLink(), systemResponse.getStatusCode());
			return;
		}

		ResponseEntity<SystemAnnotation> annResponse = restTemplate.getForEntity(addProtocolIfMissing(link.getAnnotationLink()), SystemAnnotation.class);
		if (annResponse.getStatusCode() != HttpStatus.OK) {
			LOGGER.error("Could not retrieve annotation from {}. Got response code {}!", link.getAnnotationLink(), annResponse.getStatusCode());
			return;
		}

		AnnotationValidityReport report;

		try {
			report = storageManager.createOrUpdate(link.getTag(), systemResponse.getBody(), annResponse.getBody());
		} catch (IOException e) {
			LOGGER.error("Error during storing the new system model and annotation with tag {}!", link.getTag());
			e.printStackTrace();
			return;
		}

		if (!report.isOk()) {
			amqpTemplate.convertAndSend(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME, "report", report);
		}
	}

	private String addProtocolIfMissing(String url) {
		if (url.startsWith("http")) {
			return url;
		} else {
			return "http://" + url;
		}
	}

}
