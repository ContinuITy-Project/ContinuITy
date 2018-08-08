package org.continuity.orchestrator.entities;

import java.util.function.Function;

import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

public class CreationStep implements RecipeStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreationStep.class);

	private final AmqpTemplate amqpTemplate;

	private final ExchangeDefinition<?> exchange;

	private final String routingKey;

	private final String name;

	private final Function<LinkExchangeModel, Boolean> dataAlreadyPresent;

	private TaskDescription task;

	public CreationStep(String name, AmqpTemplate amqpTemplate, ExchangeDefinition<?> exchange, String routingKey, Function<LinkExchangeModel, Boolean> dataAlreadyPresent) {
		this.name = name;
		this.amqpTemplate = amqpTemplate;
		this.exchange = exchange;
		this.routingKey = routingKey;
		this.dataAlreadyPresent = dataAlreadyPresent;
	}

	@Override
	public boolean checkData(LinkExchangeModel source) {
		boolean dataPresent = (source != null) && dataAlreadyPresent.apply(source);

		if (dataPresent) {
			LOGGER.info("Task {}: The data is already present.", name);
		} else {
			LOGGER.info("Task {}: The data is not present, yet.", name);
		}

		return dataPresent;
	}

	@Override
	public void execute() {
		LOGGER.info("Sending creation task with ID {} to {}", task.getTaskId(), exchange);

		amqpTemplate.convertAndSend(exchange.name(), routingKey, task);
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
