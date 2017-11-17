package org.continuity.workload.annotation.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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

	public static final String MODEL_CREATED_QUEUE_NAME = "model-created-annotation";

	private static final String MODEL_CREATED_EXCHANGE_NAME = "model-created";

	private static final String MODEL_CREATED_ROUTING_KEY = "*";

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	// Input queue

	@Bean
	Queue behaviorExtractedQueue() {
		return new Queue(MODEL_CREATED_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange behaviorExtractedExchange() {
		return new TopicExchange(MODEL_CREATED_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding behaviorExtractedBinding() {
		return BindingBuilder.bind(behaviorExtractedQueue()).to(behaviorExtractedExchange()).with(MODEL_CREATED_ROUTING_KEY);
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

}
