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

	private WessbasPipelineManager pipelineManager = new WessbasPipelineManager(this::handleModelCreated);

	@RabbitListener(queues = RabbitMqConfig.MONITORING_DATA_AVAILABLE_QUEUE_NAME)
	public void onMonitoringDataAvailable(MonitoringData data) {
		System.out.println("Received monitoring data: " + data);

		pipelineManager.runPipeline(data);
	}

	private void handleModelCreated(WorkloadModel workloadModel) {
		String id = SimpleModelStorage.instance().put(workloadModel);
		sendCreated(id);
	}

	private void sendCreated(String id) {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			hostname = "UNKNOWN";
		}

		String base = hostname + "/wessbas";
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas", new WorkloadModelPack(base, "/model/" + id, "/annotation/system/" + id, "/annotation/annotation/" + id));
	}

}
