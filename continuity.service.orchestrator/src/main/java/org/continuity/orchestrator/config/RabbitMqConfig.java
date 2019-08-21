package org.continuity.orchestrator.config;

import org.continuity.api.amqp.AmqpApi;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RabbitMqConfig {

	public static final String SERVICE_NAME = "orchestrator";

	public static final String EVENT_FINISHED_QUEUE_NAME = "continuity.orchestrator.event.global.finished";

	public static final String EVENT_FINISHED_ROUTING_KEY = AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of("*");

	public static final String EVENT_FAILED_QUEUE_NAME = "continuity.orchestrator.event.global.failed";

	public static final String DEAD_LETTER_QUEUE_NAME = AmqpApi.DEAD_LETTER_EXCHANGE.deriveQueueName(SERVICE_NAME);

	@Bean
	MessageConverter jsonMessageConverter(ObjectMapper mapper) {
		return new Jackson2JsonMessageConverter(mapper);
	}

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	@Bean
	AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(converter);
		rabbitTemplate.setBeforePublishPostProcessors(typeRemovingProcessor());

		return rabbitTemplate;
	}

	@Bean
	Queue eventFinishedQueue() {
		return QueueBuilder.nonDurable(EVENT_FINISHED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	TopicExchange eventFinishedExchange() {
		return AmqpApi.Global.EVENT_FINISHED.create();
	}

	@Bean
	Binding eventFinishedBinding() {
		return BindingBuilder.bind(eventFinishedQueue()).to(eventFinishedExchange()).with(EVENT_FINISHED_ROUTING_KEY);
	}

	@Bean
	TopicExchange eventRecipeFinishedExchange() {
		return AmqpApi.Orchestrator.EVENT_FINISHED.create();
	}

	@Bean
	TopicExchange eventConfigAvailableExchange() {
		return AmqpApi.Orchestrator.EVENT_CONFIG_AVAILABLE.create();
	}

	@Bean
	Queue eventFailedQueue() {
		return QueueBuilder.nonDurable(EVENT_FAILED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	TopicExchange eventFailedExchange() {
		return AmqpApi.Global.EVENT_FAILED.create();
	}

	@Bean
	Binding eventFailedBinding() {
		return BindingBuilder.bind(eventFailedQueue()).to(eventFailedExchange()).with("#");
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
