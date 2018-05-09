package org.continuity.idpa.annotation.config;

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
 * @author Henning Schulz
 *
 */
@Configuration
public class RabbitMqConfig {

	public static final String SERVICE_NAME = "idpaannotation";

	public static final String WORKLOAD_MODEL_CREATED_QUEUE_NAME = "continuity.idpaannotation.workload.model.created";

	public static final String WORKLOAD_MODEL_CREATED_ROUTING_KEY = "#";

	public static final String IDPA_APPLICATION_CHANGED_QUEUE_NAME = "continuity.idpaannotation.idpaapplication.application.changed";

	public static final String IDPA_APPLICATION_CHANGED_ROUTING_KEY = AmqpApi.IdpaApplication.APPLICATION_CHANGED.formatRoutingKey().of("*");

	public static final String DEAD_LETTER_QUEUE_NAME = AmqpApi.DEAD_LETTER_EXCHANGE.deriveQueueName(SERVICE_NAME);

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	@Bean
	TopicExchange messageAvailableExchange() {
		return AmqpApi.IdpaAnnotation.MESSAGE_AVAILABLE.create();
	}

	@Bean
	TopicExchange workloadModelCreatedExchange() {
		return AmqpApi.Workload.MODEL_CREATED.create();
	}

	@Bean
	Queue workloadModelCreatedQueue() {
		return QueueBuilder.nonDurable(WORKLOAD_MODEL_CREATED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding workloadModelCreatedBinding() {
		return BindingBuilder.bind(workloadModelCreatedQueue()).to(workloadModelCreatedExchange()).with(WORKLOAD_MODEL_CREATED_ROUTING_KEY);
	}

	@Bean
	TopicExchange idpaApplicationChangedExchange() {
		return AmqpApi.IdpaApplication.APPLICATION_CHANGED.create();
	}

	@Bean
	Queue idpaApplicationChangedQueue() {
		return QueueBuilder.nonDurable(IDPA_APPLICATION_CHANGED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding idpaApplicationChangedBinding() {
		return BindingBuilder.bind(idpaApplicationChangedQueue()).to(idpaApplicationChangedExchange()).with(IDPA_APPLICATION_CHANGED_ROUTING_KEY);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory containerFactory(ConnectionFactory connectionFactory, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(jsonMessageConverter());
		factory.setAfterReceivePostProcessors(typeRemovingProcessor());
		return factory;
	}

	@Bean
	AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter());
		rabbitTemplate.setBeforePublishPostProcessors(typeRemovingProcessor());

		return rabbitTemplate;
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
