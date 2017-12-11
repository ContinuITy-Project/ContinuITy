package org.continuity.wessbas.amqp;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
import org.springframework.stereotype.Component;

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

		String storageId = SimpleModelStorage.instance().reserve(data.getTag());
		WessbasPipelineManager pipelineManager = new WessbasPipelineManager(model -> handleModelCreated(storageId, data.getTag(), model));
		pipelineManager.runPipeline(data);

		return getHostname() + "/model/" + storageId;
	}

	private void handleModelCreated(String storageId, String tag, WorkloadModel workloadModel) {
		SimpleModelStorage.instance().put(storageId, workloadModel);
		sendCreated(storageId, tag);
	}

	private void sendCreated(String id, String tag) {
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas", new WorkloadModelPack(getHostname(), id, tag));
	}

	private String getHostname() {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.error("Could not get hostname! Returning 'UNKNOWN'.");
			e.printStackTrace();
			hostname = "UNKNOWN";
		}

		return hostname;
	}

}
