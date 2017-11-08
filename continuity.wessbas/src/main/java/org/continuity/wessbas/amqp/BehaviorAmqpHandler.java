package org.continuity.wessbas.amqp;

import org.continuity.wessbas.adapters.WessbasPipelineManager;
import org.continuity.wessbas.config.RabbitMqConfig;
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
public class BehaviorAmqpHandler {

	@Autowired
	private ModelCreatedSender sender;

	private WessbasPipelineManager pipelineManager = new WessbasPipelineManager(this::handleModelCreated);

	@RabbitListener(queues = RabbitMqConfig.BEHAVIOR_EXTRACTED_QUEUE_NAME)
	public void onBehaviorExtracted(Object message) {
		System.out.println("Received message: " + message);

		pipelineManager.runPipeline();
	}

	private void handleModelCreated(WorkloadModel workloadModel) {
		String id = SimpleModelStorage.instance().put(workloadModel);
		sender.sendCreated(id);
	}

}
