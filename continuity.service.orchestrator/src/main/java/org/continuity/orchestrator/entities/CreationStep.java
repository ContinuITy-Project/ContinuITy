package org.continuity.orchestrator.entities;

import java.util.function.Function;
import java.util.function.Predicate;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.orchestrator.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

public class CreationStep implements RecipeStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreationStep.class);

	private final AmqpTemplate amqpTemplate;

	private final ExchangeDefinition<?> exchange;

	private final String routingKey;

	private final String name;

	private final String orderId;

	private final String recipeId;

	private final Function<LinkExchangeModel, Boolean> dataAlreadyPresent;

	private final String requiredService;

	private final Predicate<String> isServiceAvailable;

	private TaskDescription task;

	public CreationStep(String name, String orderId, String recipeId, AmqpTemplate amqpTemplate, ExchangeDefinition<?> exchange, String routingKey,
			Function<LinkExchangeModel, Boolean> dataAlreadyPresent, String requiredService, Predicate<String> isServiceAvailable) {
		this.name = name;
		this.orderId = orderId;
		this.recipeId = recipeId;
		this.amqpTemplate = amqpTemplate;
		this.exchange = exchange;
		this.routingKey = routingKey;
		this.dataAlreadyPresent = dataAlreadyPresent;
		this.requiredService = requiredService;
		this.isServiceAvailable = isServiceAvailable;
	}

	@Override
	public boolean checkData(LinkExchangeModel source) {
		boolean dataPresent = (source != null) && dataAlreadyPresent.apply(source);

		if (dataPresent) {
			LOGGER.info("{} Step {}: The data is already present.", LoggingUtils.formatPrefix(orderId, recipeId), name);
		} else {
			LOGGER.info("{} Step {}: The data is not present yet.", LoggingUtils.formatPrefix(orderId, recipeId), name);
		}

		return dataPresent;
	}

	@Override
	public void execute() {
		if (isServiceAvailable.test(requiredService)) {
			LOGGER.info("{} Sending creation task {}", LoggingUtils.formatPrefix(orderId, recipeId, task.getTaskId()), exchange);

			amqpTemplate.convertAndSend(exchange.name(), routingKey, task);
		} else {
			LOGGER.error("{} Cannot send creation task {}! Service {} is not available.", LoggingUtils.formatPrefix(orderId, recipeId, task.getTaskId()), exchange, requiredService);

			amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(orderId),
					OrderReport.asError(orderId, new LinkExchangeModel(), String.format("Service %s is not available!", requiredService)));
		}
	}

	@Override
	public void setTask(TaskDescription task) {
		this.task = task;
	}

	@Override
	public String getName() {
		return name;
	}

}
