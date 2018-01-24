package org.continuity.wessbas.amqp;

import java.util.Date;

import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.controllers.WessbasModelController;
import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.continuity.wessbas.managers.WessbasPipelineManager;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;

/**
 * Handles received monitoring data in order to create WESSBAS models.
 *
 * @author Henning Schulz
 *
 */
@Component
public class WessbasAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#MONITORING_DATA_AVAILABLE_QUEUE_NAME}. Creates
	 * a new WESSBAS model based on the specified monitoring data.
	 *
	 * @param data
	 *            The data to be transformed into a WESSBAS model.
	 * @return The id that can be used to retrieve the created model later on.
	 * @see WessbasModelController
	 */
	@RabbitListener(queues = RabbitMqConfig.MONITORING_DATA_AVAILABLE_QUEUE_NAME)
	public void onMonitoringDataAvailable(MonitoringData data) {
		LOGGER.info("Received new monitoring data '{}' to be processed for '{}'", data.getDataLink(), data.getStorageLink());

		String storageId = extractStorageId(data.getStorageLink());

		if (storageId == null) {
			LOGGER.error("Storage link {} was not properly formed! Aborting.", data.getStorageLink());
		}

		WessbasPipelineManager pipelineManager = new WessbasPipelineManager(model -> handleModelCreated(storageId, model, data.getTimestamp()), restTemplate);
		pipelineManager.runPipeline(data);

		LOGGER.info("Created a new workload model with id '{}'.", storageId);
	}

	private void handleModelCreated(String storageId, WorkloadModel workloadModel, Date timestamp) {
		WorkloadModelPack responsePack;
		String tag = extractTag(storageId);

		if (workloadModel == null) {
			responsePack = WorkloadModelPack.asError(applicationName, storageId, tag);
		} else {
			SimpleModelStorage.instance().put(storageId, timestamp, workloadModel);
			responsePack = new WorkloadModelPack(applicationName, storageId, tag);
		}

		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas.wessbas/model/" + storageId, responsePack);
	}

	private String extractStorageId(String storageLink) {
		String[] tokens = storageLink.split("/");

		if (tokens.length != 3) {
			return null;
		} else if (!tokens[2].matches(".+-\\d+")) {
			return null;
		} else {
			return tokens[2];
		}
	}

	private String extractTag(String storageId) {
		return storageId.substring(0, storageId.lastIndexOf("-"));
	}

}
