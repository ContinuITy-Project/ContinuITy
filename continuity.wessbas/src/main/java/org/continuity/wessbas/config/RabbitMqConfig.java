package org.continuity.wessbas.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
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

	public static final String BEHAVIOR_EXTRACTED_QUEUE_NAME = "wessbas-model-generator-input";

	private static final String BEHAVIOR_EXTRACTED_EXCHANGE_NAME = "wessbas-behavior-extracted";

	public static final String MODEL_CREATED_EXCHANGE_NAME = "model-created";

	// Input queue

	@Bean
	Queue behaviorExtractedQueue() {
		return new Queue(BEHAVIOR_EXTRACTED_QUEUE_NAME, false);
	}

	@Bean
	FanoutExchange behaviorExtractedExchange() {
		return new FanoutExchange(BEHAVIOR_EXTRACTED_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding behaviorExtractedBinding() {
		return BindingBuilder.bind(behaviorExtractedQueue()).to(behaviorExtractedExchange());
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
		return factory;
	}

	// Output exchange

	@Bean
	public TopicExchange modelCreatedExchange() {
		return new TopicExchange(MODEL_CREATED_EXCHANGE_NAME, false, true);
	}

	@Bean
	public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter());
		rabbitTemplate.setBeforePublishPostProcessors(m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		});
		return rabbitTemplate;
	}

}
