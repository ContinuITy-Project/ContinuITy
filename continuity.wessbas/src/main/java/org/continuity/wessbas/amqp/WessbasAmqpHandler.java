package org.continuity.wessbas.amqp;

import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.managers.WessbasPipelineManager;
import org.continuity.wessbas.storage.SimpleModelStorage;
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
	private ModelCreatedSender sender;

	private WessbasPipelineManager pipelineManager = new WessbasPipelineManager(this::handleModelCreated);

	@RabbitListener(queues = RabbitMqConfig.MONITORING_DATA_AVAILABLE_QUEUE_NAME)
	public void onMonitoringDataAvailable(MonitoringData data) {
		System.out.println("Received monitoring data: " + data);

		pipelineManager.runPipeline(data);
	}

	private void handleModelCreated(WorkloadModel workloadModel) {
		String id = SimpleModelStorage.instance().put(workloadModel);
		sender.sendCreated(id);
	}

}
