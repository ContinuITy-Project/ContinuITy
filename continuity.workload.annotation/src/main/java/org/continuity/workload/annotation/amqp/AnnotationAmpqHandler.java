package org.continuity.workload.annotation.amqp;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.workload.annotation.config.RabbitMqConfig;
import org.continuity.workload.annotation.entities.AnnotationValidityReport;
import org.continuity.workload.annotation.entities.WorkloadModelLink;
import org.continuity.workload.annotation.storage.AnnotationStorage;
import org.continuity.workload.annotation.validation.AnnotationValidityChecker;
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

	private final AnnotationStorage storage;

	private final RestTemplate restTemplate;

	private final AmqpTemplate amqpTemplate;

	@Autowired
	public AnnotationAmpqHandler(AnnotationStorage storage, RestTemplate restTemplate, AmqpTemplate amqpTemplate) {
		this.storage = storage;
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

		SystemModel oldSystemModel;
		try {
			oldSystemModel = storage.readSystemModel(link.getTag());
		} catch (IOException e) {
			LOGGER.error("Could not read system model with tag {}!", link.getTag());
			e.printStackTrace();
			return;
		}

		if (oldSystemModel != null) {
			AnnotationValidityChecker checker = new AnnotationValidityChecker(systemResponse.getBody());
			checker.checkOldSystemModel(oldSystemModel);
			checker.checkAnnotation(annResponse.getBody());
			AnnotationValidityReport report = checker.getReport();

			if (!report.isOk()) {
				amqpTemplate.convertAndSend(RabbitMqConfig.CLIENT_MESSAGE_EXCHANGE_NAME, "report", report);
			}

			if (report.isBreaking()) {
				LOGGER.warn("The passed annotation with tag {} is breaking! Did not store it.", link.getTag());
				return;
			}
		}

		boolean overwritten;

		try {
			overwritten = storage.saveOrUpdate(link.getTag(), systemResponse.getBody());
			storage.saveIfNotPresent(link.getTag(), annResponse.getBody());
		} catch (IOException e) {
			LOGGER.error("Could not save annotation with tag {}!", link.getTag());
			e.printStackTrace();
			return;
		}

		if (overwritten) {
			LOGGER.info("Stored annotation with tag {} in existing directory. Old system model has been overwritten.", link.getTag());
		} else {
			LOGGER.info("Stored annotation with tag {} in new directory.", link.getTag());
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
