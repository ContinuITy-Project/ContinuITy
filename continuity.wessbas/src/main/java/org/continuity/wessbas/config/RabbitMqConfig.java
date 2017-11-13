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

	public static final String MONITORING_DATA_AVAILABLE_QUEUE_NAME = "wessbas-monitoring-input";

	private static final String MONITORING_DATA_AVAILABLE_EXCHANGE_NAME = "monitoring-data-available";

	private static final String MONITORING_DATA_AVAILABLE_ROUTING_KEY = "wessbas";

	public static final String MODEL_CREATED_EXCHANGE_NAME = "model-created";

	public static final String MODEL_CREATED_ROUTING_KEY = "wessbas";

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

	@Bean
	SimpleRabbitListenerContainerFactory containerFactory(ConnectionFactory connectionFactory, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(jsonMessageConverter());
		factory.setAfterReceivePostProcessors(typeRemovingProcessor());
		return factory;
	}

	// Output exchange

	@Bean
	TopicExchange modelCreatedExchange() {
		return new TopicExchange(MODEL_CREATED_EXCHANGE_NAME, false, true);
	}

	@Bean
	AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter());
		rabbitTemplate.setBeforePublishPostProcessors(typeRemovingProcessor());

		return rabbitTemplate;
	}

}
