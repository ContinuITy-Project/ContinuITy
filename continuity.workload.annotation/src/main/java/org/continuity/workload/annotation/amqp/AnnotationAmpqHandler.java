package org.continuity.workload.annotation.amqp;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.workload.annotation.config.RabbitMqConfig;
import org.continuity.workload.annotation.entities.WorkloadModelLink;
import org.continuity.workload.annotation.storage.AnnotationStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	@Autowired
	private AnnotationStorage storage;

	@Autowired
	private RestTemplate restTemplate;

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

		// TODO: check if system has changed and adopt annotation

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
