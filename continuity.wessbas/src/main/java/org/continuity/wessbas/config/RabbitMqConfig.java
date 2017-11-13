package org.continuity.wessbas.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
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

	// Model creation input and output queues

	public static final String MONITORING_DATA_AVAILABLE_QUEUE_NAME = "wessbas-monitoring-input";

	private static final String MONITORING_DATA_AVAILABLE_EXCHANGE_NAME = "monitoring-data-available";

	private static final String MONITORING_DATA_AVAILABLE_ROUTING_KEY = "wessbas";

	public static final String MODEL_CREATED_EXCHANGE_NAME = "model-created";

	public static final String MODEL_CREATED_ROUTING_KEY = "wessbas";

	// Load test creation input and output queues

	public static final String LOAD_TEST_NEEDED_QUEUE_NAME = "jmeter-wessbas-test-needed";

	public static final String LOAD_TEST_NEEDED_EXCHANGE_NAME = "load-test-needed";

	public static final String LOAD_TEST_NEEDED_ROUTING_KEY = "wessbas.jmeter";

	/**
	 * routing keys: [workload-type].[load-test-type], e.g., wessbas.benchflow
	 */
	public static final String LOAD_TEST_CREATED_EXCHANGE_NAME = "load-test-created";

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

	// Input exchange and queue

	@Bean
	Queue monitoringDataAvailableQueue() {
		return new Queue(MONITORING_DATA_AVAILABLE_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange monitoringDataAvailableExchange() {
		return new TopicExchange(MONITORING_DATA_AVAILABLE_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding behaviorExtractedBinding() {
		return BindingBuilder.bind(monitoringDataAvailableQueue()).to(monitoringDataAvailableExchange()).with(MONITORING_DATA_AVAILABLE_ROUTING_KEY);
	}

	// Model output exchange

	@Bean
	TopicExchange modelCreatedExchange() {
		return new TopicExchange(MODEL_CREATED_EXCHANGE_NAME, false, true);
	}

	// Load test needed input

	@Bean
	Queue loadTestNeededQueue() {
		return new Queue(LOAD_TEST_NEEDED_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange loadTestNeededExchange() {
		return new TopicExchange(LOAD_TEST_NEEDED_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding loadTestNeededBinding() {
		return BindingBuilder.bind(loadTestNeededQueue()).to(loadTestNeededExchange()).with(LOAD_TEST_NEEDED_ROUTING_KEY);
	}

	// Load test output exchange

	@Bean
	TopicExchange loadTestCreatedExchange() {
		return new TopicExchange(LOAD_TEST_CREATED_EXCHANGE_NAME, false, true);
	}

}
