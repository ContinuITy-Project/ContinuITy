package org.continuity.orchestrator.amqp;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.orchestrator.config.RabbitMqConfig;
import org.continuity.orchestrator.entities.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestrationAmqpHandler.class);

	@Autowired
	private MemoryStorage<Recipe> storage;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RabbitListener(queues = RabbitMqConfig.EVENT_FINISHED_QUEUE_NAME)
	public void onTaskFinished(TaskReport report) {
		LOGGER.info("Received finished task {}.", report.getTaskId());

		String recipeId = report.getTaskId().split("\\.")[0];
		Recipe recipe = storage.get(recipeId);

		if (recipe == null) {
			LOGGER.error("There is no recipe with ID {}!", report.getTaskId());
			return;
		}

		if (!report.isSuccessful()) {
			LOGGER.warn("The report for task {} is errorenous: {}", report.getTaskId(), report.getError());
			finishRecipe(OrderReport.asError(recipeId, recipe.getSource(), report.getError().toString()));
			return;
		}

		recipe.updateFromReport(report);

		if (recipe.hasNext()) {
			recipe.next().execute();
		} else {
			finishRecipe(OrderReport.asSuccessful(recipe.getRecipeId(), recipe.getSource()));
		}
	}

	private void finishRecipe(OrderReport report) {
		amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(report.getOrderId()), report);

		storage.remove(report.getOrderId());

		LOGGER.info("Sent recipe {} to finished queue.", report.getOrderId());
	}

}
