package org.continuity.orchestrator.entities;

import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.entities.config.TaskDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

public class CreationStep implements RecipeStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreationStep.class);

	private final AmqpTemplate amqpTemplate;

	private final ExchangeDefinition<?> exchange;

	private final String routingKey;

	private final String name;

	private TaskDescription task;

	public CreationStep(String name, AmqpTemplate amqpTemplate, ExchangeDefinition<?> exchange, String routingKey) {
		this.name = name;
		this.amqpTemplate = amqpTemplate;
		this.exchange = exchange;
		this.routingKey = routingKey;
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
