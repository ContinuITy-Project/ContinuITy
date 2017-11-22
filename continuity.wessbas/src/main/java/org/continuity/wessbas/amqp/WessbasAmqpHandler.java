package org.continuity.wessbas.amqp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.continuity.wessbas.managers.WessbasPipelineManager;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
@Component
public class WessbasAmqpHandler {

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RabbitListener(queues = RabbitMqConfig.MONITORING_DATA_AVAILABLE_QUEUE_NAME)
	public String onMonitoringDataAvailable(MonitoringData data) {
		System.out.println("Received monitoring data: " + data);

		String storageId = SimpleModelStorage.instance().reserve();
		WessbasPipelineManager pipelineManager = new WessbasPipelineManager(model -> handleModelCreated(storageId, model));
		pipelineManager.runPipeline(data);

		return getModelLink(storageId);
	}

	private void handleModelCreated(String storageId, WorkloadModel workloadModel) {
		SimpleModelStorage.instance().put(storageId, workloadModel);
		sendCreated(storageId);
	}

	private void sendCreated(String id) {
		String base = getModelLink(id);
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas", new WorkloadModelPack(base, "/workload", "/system", "/annotation"));
	}

	private String getModelLink(String id) {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			hostname = "UNKNOWN";
		}

		return hostname + "/wessbas/" + id;
	}

}
