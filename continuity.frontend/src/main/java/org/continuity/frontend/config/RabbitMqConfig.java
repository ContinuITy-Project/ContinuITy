package org.continuity.frontend.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
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

	/**
	 * routing keys: [workload-type].[load-test-type], e.g., wessbas.benchflow
	 */
	public static final String LOAD_TEST_NEEDED_EXCHANGE_NAME = "load-test-needed";

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
		return new TopicExchange(LOAD_TEST_NEEDED_EXCHANGE_NAME, false, true);
	}

}
