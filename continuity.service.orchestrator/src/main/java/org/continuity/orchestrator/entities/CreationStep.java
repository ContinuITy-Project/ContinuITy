package org.continuity.orchestrator.entities;

import java.util.function.Predicate;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.orchestrator.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

public class CreationStep implements RecipeStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreationStep.class);

	private final AmqpTemplate amqpTemplate;

	private final String orderId;

	private final String recipeId;

	private final ArtifactType target;

	private final String service;

	private final Predicate<String> isServiceAvailable;

	private TaskDescription task;

	public CreationStep(ArtifactType target, String orderId, String recipeId, AmqpTemplate amqpTemplate, String service, Predicate<String> isServiceAvailable) {
		this.orderId = orderId;
		this.recipeId = recipeId;
		this.target = target;
		this.amqpTemplate = amqpTemplate;
		this.service = service;
		this.isServiceAvailable = isServiceAvailable;
	}

	@Override
	public boolean checkData(ArtifactExchangeModel source) {
		boolean dataPresent = (source != null) && source.isPresent(target);

		if (dataPresent) {
			LOGGER.info("{} Step 'create {}': The data is already present.", LoggingUtils.formatPrefix(orderId, recipeId), target.toPrettyString());
		} else {
			LOGGER.info("{} Step 'create {}': The data is not present yet.", LoggingUtils.formatPrefix(orderId, recipeId), target.toPrettyString());
		}

		return dataPresent;
	}

	@Override
	public void execute() {
		if (isServiceAvailable.test(service)) {
			LOGGER.info("{} Sending creation task for target {} to {}", LoggingUtils.formatPrefix(orderId, recipeId, task.getTaskId()), target, service);

			if (task != null) {
				task.setTarget(target);
			}

			amqpTemplate.convertAndSend(AmqpApi.Global.TASK_CREATE.name(), AmqpApi.Global.TASK_CREATE.formatRoutingKey().of(service, target), task);
		} else {
			LOGGER.error("{} Cannot send creation task for target {}! Service {} is not available.", LoggingUtils.formatPrefix(orderId, recipeId, task.getTaskId()), target, service);

			amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(orderId),
					OrderReport.asError(orderId, new ArtifactExchangeModel(), String.format("Service %s is not available!", service)));
		}
	}

	@Override
	public void setTask(TaskDescription task) {
		this.task = task;
	}

	@Override
	public String getName() {
		return target.toPrettyString();
	}

}
