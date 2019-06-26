package org.continuity.wessbas.config;

import org.continuity.api.amqp.AmqpApi;
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

/**
 * @author Henning Schulz, Alper Hidiroglu
 *
 */
@Configuration
public class RabbitMqConfig {

	public static final String SERVICE_NAME = "wessbas";

	public static final String TASK_CREATE_QUEUE_NAME = "continuity.wessbas.task.workloadmodel.create";
	
	public static final String MIX_CREATE_QUEUE_NAME = "continuity.wessbas.task.behaviormix.createmix";

	public static final String TASK_CREATE_ROUTING_KEY = AmqpApi.WorkloadModel.TASK_CREATE.formatRoutingKey().of(SERVICE_NAME);
	
	public static final String MIX_CREATE_ROUTING_KEY = AmqpApi.WorkloadModel.MIX_CREATE.formatRoutingKey().of(SERVICE_NAME);

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
		return AmqpApi.WorkloadModel.TASK_CREATE.create();
	}
	
	@Bean
	TopicExchange mixCreateExchange() {
		return AmqpApi.WorkloadModel.MIX_CREATE.create();
	}

	@Bean
	Queue taskCreateQueue() {
		return QueueBuilder.nonDurable(TASK_CREATE_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}
	
	@Bean
	Queue mixCreateQueue() {
		return QueueBuilder.nonDurable(MIX_CREATE_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding taskCreateBinding() {
		return BindingBuilder.bind(taskCreateQueue()).to(taskCreateExchange()).with(TASK_CREATE_ROUTING_KEY);
	}
	
	@Bean
	Binding mixCreateBinding() {
		return BindingBuilder.bind(mixCreateQueue()).to(mixCreateExchange()).with(MIX_CREATE_ROUTING_KEY);
	}

	@Bean
	TopicExchange eventCreatedExchange() {
		return AmqpApi.WorkloadModel.EVENT_CREATED.create();
	}

	@Bean
	TopicExchange finishedExchange() {
		return AmqpApi.Global.EVENT_FINISHED.create();
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
