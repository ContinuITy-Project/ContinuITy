package org.continuity.wessbas.amqp;

import java.io.IOException;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.BehaviorModelType;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.managers.WessbasPipelineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Creates Markov-chain-based behavior models using WESSBAS from session logs.
 *
 * @author Henning Schulz
 *
 */
@Component
public class BehaviorModelAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorModelAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MixedStorage<BehaviorModelPack> storage;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_BEHAVIOR_QUEUE_NAME)
	public void createBehaviorModel(TaskDescription task) throws IOException {
		LOGGER.info("Task {}: Received new task for creating a behavior model for app-id '{}'", task.getTaskId(), task.getAppId());

		TaskReport report;

		if (task.getSource().getSessionLinks().getExtendedLink() == null) {
			LOGGER.error("Task {}: Session logs link and forecast link is missing for app-id {}!", task.getTaskId(), task.getAppId());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			WessbasPipelineManager pipelineManager = new WessbasPipelineManager(restTemplate);
			// TODO: retrieve intensity from service configuration
			BehaviorModelPack behaviorModel = pipelineManager.createBehaviorModelFromSessions(task, 60000000000L);

			String storageId = storage.put(behaviorModel, task.getAppId(), task.isLongTermUse());
			String behaviorModelLink = RestApi.Wessbas.BehaviorModel.GET.requestUrl(storageId).withoutProtocol().get();

			report = TaskReport.successful(task.getTaskId(), new ArtifactExchangeModel().getBehaviorModelLinks().setLink(behaviorModelLink).setType(BehaviorModelType.MARKOV_CHAIN).parent());

			LOGGER.info("Task {}: Created a new sessions-bundle-pack with id '{}'.", task.getTaskId(), storageId);
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
