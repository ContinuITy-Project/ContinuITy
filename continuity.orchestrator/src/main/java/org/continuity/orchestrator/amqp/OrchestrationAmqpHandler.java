package org.continuity.orchestrator.amqp;

import java.io.IOException;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.orchestrator.config.RabbitMqConfig;
import org.continuity.orchestrator.entities.Recipe;
import org.continuity.orchestrator.storage.TestingContextStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestrationAmqpHandler.class);

	@Autowired
	private MemoryStorage<Recipe> storage;

	@Autowired
	@Qualifier("testingContextStorage")
	private TestingContextStorage testingContextStorage;

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
			OrderReport orderReport = OrderReport.asSuccessful(recipe.getOrderId(), recipe.getTestingContext(), recipe.getSource());
			storeToTestingContext(orderReport, recipe.getTag());
			finishRecipe(orderReport);
		}
	}

	@RabbitListener(queues = RabbitMqConfig.EVENT_FAILED_QUEUE_NAME)
	public void onTaskFailed(TaskDescription description, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
		if (RabbitMqConfig.SERVICE_NAME.equals(routingKey)) {
			return;
		}

		LOGGER.warn("Received failed task {}.", description.getTaskId());

		String recipeId = description.getTaskId().split("\\.")[0];
		Recipe recipe = storage.get(recipeId);

		if (recipe == null) {
			LOGGER.error("There is no recipe with ID {}!", description.getTaskId());
			return;
		}

		String error;

		if (routingKey == null) {
			error = "Unknown service failed.";
		} else {
			error = "Service " + routingKey + " failed.";
		}

		finishRecipe(OrderReport.asError(recipeId, recipe.getSource(), error));
		LOGGER.info("Finished failed recipe {}.", recipeId);
	}

	private void finishRecipe(OrderReport report) {
		amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(report.getOrderId()), report);

		storage.remove(report.getOrderId());

		LOGGER.info("Sent recipe {} to finished queue.", report.getOrderId());
	}

	private void storeToTestingContext(OrderReport report, String tag) {
		if ((report.getTestingContext() != null) && !report.getTestingContext().isEmpty()) {
			try {
				testingContextStorage.store(tag, report.getTestingContext(), report.getInternalArtifacts());
			} catch (IOException e) {
				LOGGER.error("Error when storing the testing context!", e);
			}
		}
	}

}
