package org.continuity.wessbas.amqp;

import java.io.IOException;
import java.util.List;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.exchange.BehaviorModelType;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.controllers.WessbasModelController;
import org.continuity.wessbas.entities.BehaviorModelPack;
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

import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;

/**
 * Handles received monitoring data in order to create WESSBAS models.
 *
 * @author Henning Schulz
 *
 */
@Component
public class WorkloadModelAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadModelAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MixedStorage<WessbasBundle> storage;

	@Autowired
	private MixedStorage<BehaviorModelPack> behaviorStorage;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#TASK_CREATE_WORKLOAD_QUEUE_NAME}. Creates a
	 * new WESSBAS model based on the specified monitoring data.
	 *
	 * @param task
	 *            The description of the task to be done.
	 * @return The id that can be used to retrieve the created model later on.
	 * @throws GeneratorException
	 * @throws IOException
	 * @throws SecurityException
	 * @see WessbasModelController
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_WORKLOAD_QUEUE_NAME)
	public void onMonitoringDataAvailable(TaskDescription task) throws SecurityException, IOException, GeneratorException {
		LOGGER.info("Task {}: Received new task for creating a workload model for app-id '{}'", task.getTaskId(), task.getAppId());

		TaskReport report;

		if (task.getSource().getBehaviorModelLinks().isEmpty()) {
			LOGGER.error("Task {}: Behavior model link is missing for app-id {}!", task.getTaskId(), task.getAppId());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else if (task.getSource().getBehaviorModelLinks().getType() != BehaviorModelType.MARKOV_CHAIN) {
			LOGGER.error("Task {}: Cannot process {} behavior model for app-id {}!", task.getTaskId(), task.getSource().getBehaviorModelLinks().getType(), task.getAppId());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
		} else {
			List<String> pathParams = RestApi.Wessbas.BehaviorModel.GET.parsePathParameters(task.getSource().getBehaviorModelLinks().getLink());
			WessbasPipelineManager pipelineManager;
			BehaviorModelPack behaviorModel;

			if ((pathParams == null) || pathParams.isEmpty()) {
				LOGGER.info("Task {}: Transforming externally created Markov behavior model to the WESSBAS format.", task.getTaskId());
				LOGGER.warn("Task {}: Service-tailoring is currently not supported in this case!", task.getTaskId());

				pipelineManager = new WessbasPipelineManager(restTemplate);
				behaviorModel = pipelineManager.createBehaviorModelFromMarkovChains(task);
			} else {
				LOGGER.info("Task {}: Using internally created behavior model.", task.getTaskId());

				behaviorModel = behaviorStorage.get(pathParams.get(0));
				pipelineManager = new WessbasPipelineManager(restTemplate, behaviorModel.getPathToBehaviorModelFiles());
			}

			WessbasBundle workloadModel = pipelineManager.transformBehaviorModelToWorkloadModelIncludingTailoring(behaviorModel, task);

			if (workloadModel == null) {
				LOGGER.info("Task {}: Could not create a new workload model for app-id '{}'.", task.getTaskId(), task.getAppId());

				report = TaskReport.error(task.getTaskId(), TaskError.INTERNAL_ERROR);
			} else {
				String storageId = storage.put(workloadModel, task.getAppId(), task.isLongTermUse());

				LOGGER.info("Task {}: Created a new workload model with id '{}'.", task.getTaskId(), storageId);

				WorkloadModelPack responsePack = new WorkloadModelPack(applicationName, storageId, task.getAppId(), TailoringUtils.doTailoring(task.getEffectiveServices()));
				report = TaskReport.successful(task.getTaskId(), responsePack);
			}
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
