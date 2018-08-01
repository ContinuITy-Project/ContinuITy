package org.continuity.wessbas.amqp;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.controllers.WessbasModelController;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.continuity.wessbas.managers.WessbasPipelineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

	@Autowired
	private MemoryStorage<WessbasBundle> storage;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#TASK_CREATE_QUEUE_NAME}. Creates a new WESSBAS
	 * model based on the specified monitoring data.
	 *
	 * @param task
	 *            The description of the task to be done.
	 * @return The id that can be used to retrieve the created model later on.
	 * @see WessbasModelController
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void onMonitoringDataAvailable(TaskDescription task) {
		LOGGER.info("Task {}: Received new task to be processed for tag '{}'", task.getTaskId(), task.getTag());

		TaskReport report;

		if (task.getSource().getSessionLogsLinks().getLink() == null) {
			LOGGER.error("Task {}: Session logs link is missing for tag {}!", task.getTaskId(), task.getTag());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			WessbasPipelineManager pipelineManager = new WessbasPipelineManager(restTemplate);
			WessbasBundle workloadModel = pipelineManager.runPipeline(task.getSource().getSessionLogsLinks().getLink());

			if (workloadModel == null) {
				LOGGER.info("Task {}: Could not create a new workload model for tag '{}'.", task.getTaskId(), task.getTag());

				report = TaskReport.error(task.getTaskId(), TaskError.INTERNAL_ERROR);
			} else {
				String storageId = storage.put(workloadModel, task.getTag());

				LOGGER.info("Task {}: Created a new workload model with id '{}'.", task.getTaskId(), storageId);

				WorkloadModelPack responsePack = new WorkloadModelPack(applicationName, storageId, task.getTag());
				report = TaskReport.successful(task.getTaskId(), responsePack);

				amqpTemplate.convertAndSend(AmqpApi.WorkloadModel.EVENT_CREATED.name(), AmqpApi.WorkloadModel.EVENT_CREATED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), responsePack);
			}
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
