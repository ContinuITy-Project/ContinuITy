package org.continuity.wessbas.amqp;

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
	public String onMonitoringDataAvailable(MonitoringData data) {
		LOGGER.info("Received new monitoring data with tag '{}' to be processed: '{}'", data.getLink(), data.getTag());

		String storageId = SimpleModelStorage.instance().reserve(data.getTag());
		WessbasPipelineManager pipelineManager = new WessbasPipelineManager(model -> handleModelCreated(storageId, data.getTag(), model), restTemplate);
		pipelineManager.runPipeline(data);

		LOGGER.info("Created a new workload model with id '{}'.", storageId);

		return applicationName + "/model/" + storageId;
	}

	private void handleModelCreated(String storageId, String tag, WorkloadModel workloadModel) {
		SimpleModelStorage.instance().put(storageId, workloadModel);
		sendCreated(storageId, tag);
	}

	private void sendCreated(String id, String tag) {
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas", new WorkloadModelPack(applicationName, id, tag));
	}

}
