package org.continuity.frontend.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RabbitMqConfig {

	public static final String MONITORING_DATA_AVAILABLE_EXCHANGE_NAME = "monitoring-data-available";

	public static final String WORKLADO_ANNOTATION_MESSAGE_EXCHANGE_NAME = "continuity.workload.annotation.message";

	public static final String WORKLADO_ANNOTATION_MESSAGE_QUEUE_NAME = "continuity.frontend.workload.annotation.message";

	public static final String WORKLADO_ANNOTATION_MESSAGE_ROUTING_KEY = "report";

	/**
	 * routing keys: [workload-type].[load-test-type], e.g., wessbas.benchflow
	 */
	public static final String EXECUTE_LOAD_TEST_EXCHANGE_NAME = "execute-load-test";

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
	TopicExchange monitoringDataAvailableExchange() {
		return new TopicExchange(MONITORING_DATA_AVAILABLE_EXCHANGE_NAME, false, true);
	}

	@Bean
	TopicExchange loadTestNeededExchange() {
		return new TopicExchange(EXECUTE_LOAD_TEST_EXCHANGE_NAME, false, true);
	}

	@Bean
	Queue workloadAnnotationMessageQueue() {
		return new Queue(WORKLADO_ANNOTATION_MESSAGE_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange workloadAnnotationMessageExchange() {
		return new TopicExchange(WORKLADO_ANNOTATION_MESSAGE_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding workloadAnnotationMessageBinding() {
		return BindingBuilder.bind(workloadAnnotationMessageQueue()).to(workloadAnnotationMessageExchange()).with(WORKLADO_ANNOTATION_MESSAGE_ROUTING_KEY);
	}

}
