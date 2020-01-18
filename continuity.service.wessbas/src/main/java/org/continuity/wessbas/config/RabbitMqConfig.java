package org.continuity.wessbas.config;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.exchange.ArtifactType;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz, Alper Hidiroglu
 *
 */
@Configuration
public class RabbitMqConfig {

	public static final String SERVICE_NAME = "wessbas";

	public static final String TASK_CREATE_WORKLOAD_QUEUE_NAME = "continuity.wessbas_workload.task.global.create";

	public static final String TASK_CREATE_BEHAVIOR_QUEUE_NAME = "continuity.wessbas_behavior.task.global.create";

	public static final String TASK_CREATE_WORKLOAD_ROUTING_KEY = AmqpApi.Global.TASK_CREATE.formatRoutingKey().of(SERVICE_NAME, ArtifactType.WORKLOAD_MODEL);

	public static final String TASK_CREATE_BEHAVIOR_ROUTING_KEY = AmqpApi.Global.TASK_CREATE.formatRoutingKey().of(SERVICE_NAME, ArtifactType.BEHAVIOR_MODEL);

	public static final String DEAD_LETTER_QUEUE_NAME = AmqpApi.DEAD_LETTER_EXCHANGE.deriveQueueName(SERVICE_NAME);

	// General

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
	SimpleRabbitListenerContainerFactory containerFactory(ConnectionFactory connectionFactory, MessageConverter converter, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(converter);
		factory.setAfterReceivePostProcessors(typeRemovingProcessor());
		return factory;
	}

	@Bean
	TopicExchange taskCreateExchange() {
		return AmqpApi.Global.TASK_CREATE.create();
	}

	@Bean
	Queue taskCreateWorkloadQueue() {
		return QueueBuilder.nonDurable(TASK_CREATE_WORKLOAD_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.Global.EVENT_FAILED.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Queue taskCreateBehaviorQueue() {
		return QueueBuilder.nonDurable(TASK_CREATE_BEHAVIOR_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.Global.EVENT_FAILED.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding taskCreateWorkloadBinding() {
		return BindingBuilder.bind(taskCreateWorkloadQueue()).to(taskCreateExchange()).with(TASK_CREATE_WORKLOAD_ROUTING_KEY);
	}

	@Bean
	Binding taskCreateBehaviorBinding() {
		return BindingBuilder.bind(taskCreateBehaviorQueue()).to(taskCreateExchange()).with(TASK_CREATE_BEHAVIOR_ROUTING_KEY);
	}

	@Bean
	TopicExchange finishedExchange() {
		return AmqpApi.Global.EVENT_FINISHED.create();
	}

	@Bean
	TopicExchange eventFailedExchange() {
		return AmqpApi.Global.EVENT_FAILED.create();
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
