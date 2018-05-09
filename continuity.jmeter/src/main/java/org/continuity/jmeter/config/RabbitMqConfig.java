package org.continuity.jmeter.config;

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

	public static final String SERVICE_NAME = "jmeter";

	public static final String LOAD_TEST_EXECUTION_REQUIRED_QUEUE_NAME = "continuity.jmeter.frontend.loadtestexecution.required";

	public static final String LOAD_TEST_EXECUTION_REQUIRED_ROUTING_KEY = AmqpApi.Frontend.LOADTESTEXECUTION_REQUIRED.formatRoutingKey().of("jmeter");

	public static final String LOAD_TEST_CREATION_AND_EXECUTION_REQUIRED_QUEUE_NAME = "continuity.jmeter.frontend.loadtestcreationandexecution.required";

	public static final String LOAD_TEST_CREATION_AND_EXECUTION_REQUIRED_ROUTING_KEY = AmqpApi.Frontend.LOADTESTCREATIONANDEXECUTION_REQUIRED.formatRoutingKey().of("*", "jmeter");

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
	TopicExchange provideReportExchange() {
		return AmqpApi.LoadTest.REPORT_AVAILABLE.create();
	}

	@Bean
	TopicExchange loadTestExecutionRequiredExchange() {
		return AmqpApi.Frontend.LOADTESTEXECUTION_REQUIRED.create();
	}

	@Bean
	Queue loadTestExecutionRequiredQueue() {
		return QueueBuilder.nonDurable(LOAD_TEST_EXECUTION_REQUIRED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding loadTestExecutionRequiredBinding() {
		return BindingBuilder.bind(loadTestExecutionRequiredQueue()).to(loadTestExecutionRequiredExchange()).with(LOAD_TEST_EXECUTION_REQUIRED_ROUTING_KEY);
	}

	@Bean
	TopicExchange loadTestCreationAndExecutionRequiredExchange() {
		return AmqpApi.Frontend.LOADTESTCREATIONANDEXECUTION_REQUIRED.create();
	}

	@Bean
	Queue loadTestCreationAndExecutionRequiredQueue() {
		return QueueBuilder.nonDurable(LOAD_TEST_CREATION_AND_EXECUTION_REQUIRED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding loadTestCreationAndExecutionRequiredBinding() {
		return BindingBuilder.bind(loadTestCreationAndExecutionRequiredQueue()).to(loadTestCreationAndExecutionRequiredExchange()).with(LOAD_TEST_CREATION_AND_EXECUTION_REQUIRED_ROUTING_KEY);
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
