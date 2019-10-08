package org.continuity.session.logs.config;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.session.logs.SessionLogsConfiguration;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	public static final String SERVICE_NAME = "sessionlogs";

	public static final String TASK_CREATE_QUEUE_NAME = "continuity.sessionlogs.task.sessionlogs.create";

	public static final String TASK_CREATE_ROUTING_KEY = "#";

	public static final String EVENT_CONFIG_AVAILABLE_NAME = "continuity.sessionlogs.event.orchestrator.configavailable";

	public static final String TASK_PROCESS_TRACES_QUEUE_NAME = "continuity.sessionlogs.task.sessionlogs.process_traces";

	public static final String TASK_PROCESS_TRACES_ROUTING_KEY = "#";

	public static final String DEAD_LETTER_QUEUE_NAME = AmqpApi.DEAD_LETTER_EXCHANGE.deriveQueueName(SERVICE_NAME);

	// General

	@Bean
	MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	@Bean
	AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter());
		rabbitTemplate.setBeforePublishPostProcessors(typeRemovingProcessor());

		return rabbitTemplate;
	}

	@Bean
	SimpleRabbitListenerContainerFactory containerFactory(ConnectionFactory connectionFactory, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(jsonMessageConverter());
		factory.setAfterReceivePostProcessors(typeRemovingProcessor());
		return factory;
	}

	@Bean
	TopicExchange taskCreateExchange() {
		return AmqpApi.SessionLogs.TASK_CREATE.create();
	}

	@Bean
	Queue taskCreateQueue() {
		return QueueBuilder.nonDurable(TASK_CREATE_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding taskCreateBinding() {
		return BindingBuilder.bind(taskCreateQueue()).to(taskCreateExchange()).with(TASK_CREATE_ROUTING_KEY);
	}

	@Bean
	TopicExchange eventConfigAvailableExchange() {
		return AmqpApi.Orchestrator.EVENT_CONFIG_AVAILABLE.create();
	}

	@Bean
	Queue eventConfigAvailableQueue() {
		return QueueBuilder.nonDurable(EVENT_CONFIG_AVAILABLE_NAME).build();
	}

	@Bean
	Binding eventConfigAvailableBinding() {
		return BindingBuilder.bind(eventConfigAvailableQueue()).to(eventConfigAvailableExchange()).with(SessionLogsConfiguration.SERVICE);
	}

	@Bean
	TopicExchange taskFinishedExchange() {
		return AmqpApi.Global.EVENT_FINISHED.create();
	}

	@Bean
	TopicExchange taskProcessTracesExchange() {
		return AmqpApi.SessionLogs.TASK_PROCESS_TRACES.create();
	}

	@Bean
	Queue taskProcessTracesQueue() {
		return QueueBuilder.nonDurable(TASK_PROCESS_TRACES_QUEUE_NAME).build();
	}

	@Bean
	Binding taskProcessTracesBinding() {
		return BindingBuilder.bind(taskProcessTracesQueue()).to(taskProcessTracesExchange()).with(TASK_PROCESS_TRACES_ROUTING_KEY);
	}

	// Dead letter exchange and queue

	@Bean
	TopicExchange deadLetterExchange() {
		return AmqpApi.DEAD_LETTER_EXCHANGE.create();
	}

	@Bean
	Queue deadLetterQueue() {
		return new Queue(DEAD_LETTER_QUEUE_NAME, true);
	}

	@Bean
	Binding deadLetterBinding() {
		return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(SERVICE_NAME);
	}

}
