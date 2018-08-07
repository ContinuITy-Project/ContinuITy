package org.continuity.orchestrator.entities;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.orchestrator.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

public class DummyStep implements RecipeStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(DummyStep.class);

	private final AmqpTemplate amqpTemplate;

	private TaskDescription task;

	public DummyStep(AmqpTemplate amqpTemplate) {
		this.amqpTemplate = amqpTemplate;
	}

	@Override
	public void execute() {
		LOGGER.warn("Dummy step for task {} - doing nothing!", task.getTaskId());

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME),
				TaskReport.successful(task.getTaskId(), new LinkExchangeModel()));
	}

	@Override
	public void setTask(TaskDescription task) {
		this.task = task;
	}

	@Override
	public String getName() {
		return "dummy";
	}

}
