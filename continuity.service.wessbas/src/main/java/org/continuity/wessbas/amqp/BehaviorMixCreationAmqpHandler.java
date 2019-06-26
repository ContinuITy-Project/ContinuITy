package org.continuity.wessbas.amqp;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.controllers.WessbasModelController;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.managers.BehaviorMixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Handles received monitoring data in order to create the Behavior mix.
 *
 * @author Alper Hidiroglu
 *
 */
@Component
public class BehaviorMixCreationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorMixCreationAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MixedStorage<BehaviorModelPack> storage;
	
//	@Autowired
//	private ConcurrentHashMap<String, Path> pathStorage;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#TASK_CREATE_QUEUE_NAME}. Creates a new Behavior
	 * mix based on the specified monitoring data.
	 *
	 * @param task
	 *            The description of the task to be done.
	 * @return The id that can be used to retrieve the mix later on.
	 * @see WessbasModelController
	 */
	@RabbitListener(queues = RabbitMqConfig.MIX_CREATE_QUEUE_NAME)
	public void onMonitoringDataAvailable(TaskDescription task) {
		LOGGER.info("Task {}: Received new task to be processed for tag '{}'", task.getTaskId(), task.getTag());

		TaskReport report;

		if (task.getSource().getSessionLogsLinks().getLink() == null) {
			LOGGER.error("Task {}: Session logs link is missing for tag {}!", task.getTaskId(), task.getTag());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			BehaviorMixManager behaviorManager = new BehaviorMixManager(restTemplate);
			SessionsBundlePack sessionsBundles = behaviorManager.runPipeline(task.getSource().getSessionLogsLinks().getLink());
			BehaviorModelPack behaviorModelPack = new BehaviorModelPack(sessionsBundles, behaviorManager.getWorkingDir());

			if (sessionsBundles == null) {
				LOGGER.info("Task {}: Could not create a new behavior mix for tag '{}'.", task.getTaskId(), task.getTag());

				report = TaskReport.error(task.getTaskId(), TaskError.INTERNAL_ERROR);
			} else {
				
				String storageId = storage.put(behaviorModelPack, task.getTag(), task.isLongTermUse());
				String behaviorModelPackLink = RestApi.Wessbas.SessionsBundles.GET.requestUrl(storageId).withoutProtocol().get();

				report = TaskReport.successful(task.getTaskId(), new LinkExchangeModel().getSessionsBundlesLinks().setLink(behaviorModelPackLink).parent());
				
				LOGGER.info("Task {}: Created a new sessions-bundle-pack with id '{}'.", task.getTaskId(), storageId);
			}
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
