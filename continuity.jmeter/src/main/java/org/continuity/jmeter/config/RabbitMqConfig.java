package org.continuity.jmeter.config;

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

	/**
	 * routing keys: [workload-type].[load-test-type], e.g., wessbas.benchflow
	 */
	public static final String EXECUTE_LOAD_TEST_EXCHANGE_NAME = "continuity.loadtest.execute";

	public static final String EXECUTE_LOAD_TEST_QUEUE_NAME = "continuity.jmeter.loadtest.execute";

	public static final String EXECUTE_LOAD_TEST_ROUTING_KEY = "jmeter";

	/**
	 * routing keys: [load-test-type], e.g., benchflow
	 */
	public static final String CREATE_AND_EXECUTE_LOAD_TEST_EXCHANGE_NAME = "continuity.loadtest.createandexecute";

	public static final String CREATE_AND_EXECUTE_LOAD_TEST_QUEUE_NAME = "continuity.jmeter.loadtest.createandexecute";

	public static final String CREATE_AND_EXECUTE_LOAD_TEST_ROUTING_KEY = "*.jmeter";

	public static final String PROVIDE_REPORT_EXCHANGE_NAME = "continuity.loadtest.report.provider";

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
	Queue executeLoadTestQueue() {
		return new Queue(EXECUTE_LOAD_TEST_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange executeLoadTestExchange() {
		return new TopicExchange(EXECUTE_LOAD_TEST_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding executeLoadTestBinding() {
		return BindingBuilder.bind(executeLoadTestQueue()).to(executeLoadTestExchange()).with(EXECUTE_LOAD_TEST_ROUTING_KEY);
	}

	@Bean
	Queue createAndExecuteLoadTestQueue() {
		return new Queue(CREATE_AND_EXECUTE_LOAD_TEST_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange createAndExecuteLoadTestExchange() {
		return new TopicExchange(CREATE_AND_EXECUTE_LOAD_TEST_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding createAndExecuteLoadTestBinding() {
		return BindingBuilder.bind(createAndExecuteLoadTestQueue()).to(createAndExecuteLoadTestExchange()).with(CREATE_AND_EXECUTE_LOAD_TEST_ROUTING_KEY);
	}

	@Bean
	TopicExchange provideReportExchange() {
		return new TopicExchange(PROVIDE_REPORT_EXCHANGE_NAME, false, true);
	}
}
