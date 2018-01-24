package org.continuity.system.annotation.config;

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

	public static final String MODEL_CREATED_QUEUE_NAME = "continuity.system.annotation.workloadmodel.created";

	public static final String MODEL_CREATED_EXCHANGE_NAME = "continuity.workloadmodel.created";

	/**
	 * routing keys: [workload-type].[workload-link], e.g., wessbas.wessbas/model/foo-1
	 */
	public static final String MODEL_CREATED_ROUTING_KEY = "#";

	public static final String CLIENT_MESSAGE_EXCHANGE_NAME = "continuity.system.annotation.message";

	public static final String SYSTEM_MODEL_CHANGED_QUEUE_NAME = "continuity.system.annotation.system.model.changed";

	public static final String SYSTEM_MODEL_CHANGED_EXCHANGE_NAME = "continuity.system.model.changed";

	/**
	 * Routing key is the tag.
	 */
	public static final String SYSTEM_MODEL_CHANGED_ROUTING_KEY = "*";

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	@Bean
	Queue behaviorExtractedQueue() {
		return new Queue(MODEL_CREATED_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange behaviorExtractedExchange() {
		// Not declaring auto delete, since queues are bound dynamically so that the exchange might
		// have no queue for a while
		return new TopicExchange(MODEL_CREATED_EXCHANGE_NAME, false, false);
	}

	@Bean
	Binding behaviorExtractedBinding() {
		return BindingBuilder.bind(behaviorExtractedQueue()).to(behaviorExtractedExchange()).with(MODEL_CREATED_ROUTING_KEY);
	}

	@Bean
	TopicExchange clientMessageExchange() {
		return new TopicExchange(CLIENT_MESSAGE_EXCHANGE_NAME, false, true);
	}

	@Bean
	Queue systemModelCreatedQueue() {
		return new Queue(SYSTEM_MODEL_CHANGED_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange systemModelCreatedExchange() {
		return new TopicExchange(SYSTEM_MODEL_CHANGED_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding systemModelCreatedBinding() {
		return BindingBuilder.bind(systemModelCreatedQueue()).to(systemModelCreatedExchange()).with(SYSTEM_MODEL_CHANGED_ROUTING_KEY);
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

}
