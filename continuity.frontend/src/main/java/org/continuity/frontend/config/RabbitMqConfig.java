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

	public static final String MONITORING_DATA_AVAILABLE_EXCHANGE_NAME = "continuity.workloadmodel.dataavailable";

	public static final String WORKLADO_ANNOTATION_MESSAGE_EXCHANGE_NAME = "continuity.workload.annotation.message";

	public static final String WORKLADO_ANNOTATION_MESSAGE_QUEUE_NAME = "continuity.frontend.workload.annotation.message";

	public static final String WORKLADO_ANNOTATION_MESSAGE_ROUTING_KEY = "report";

	public static final String PROVIDE_REPORT_EXCHANGE_NAME = "continuity.loadtest.report.provider";

	public static final String PROVIDE_REPORT_QUEUE_NAME = "continuity.jmeter.loadtest.report.provider";

	public static final String PROVIDE_REPORT_ROUTING_KEY = "jmeter";

	/**
	 * routing keys: [workload-type].[load-test-type], e.g., wessbas.benchflow
	 */
	public static final String CREATE_AND_EXECUTE_LOAD_TEST_EXCHANGE_NAME = "continuity.loadtest.createandexecute";

	/**
	 * routing keys: [load-test-type], e.g., benchflow
	 */
	public static final String EXECUTE_LOAD_TEST_EXCHANGE_NAME = "continuity.loadtest.execute";

	/**
	 * routing keys: [workload-type].[workload-link], e.g., wessbas.wessbas/model/foo-1
	 */
	public static final String WORKLOAD_MODEL_CREATED_EXCHANGE_NAME = "continuity.workloadmodel.created";

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
	TopicExchange executeLoadTestExchange() {
		return new TopicExchange(EXECUTE_LOAD_TEST_EXCHANGE_NAME, false, true);
	}

	@Bean
	TopicExchange createAndExecuteLoadTestExchange() {
		return new TopicExchange(CREATE_AND_EXECUTE_LOAD_TEST_EXCHANGE_NAME, false, true);
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

	@Bean
	Queue provideReportQueue() {
		return new Queue(PROVIDE_REPORT_QUEUE_NAME, false);
	}

	@Bean
	TopicExchange provideReportExchange() {
		return new TopicExchange(PROVIDE_REPORT_EXCHANGE_NAME, false, true);
	}

	@Bean
	Binding provideReportBinding() {
		return BindingBuilder.bind(provideReportQueue()).to(provideReportExchange()).with(PROVIDE_REPORT_ROUTING_KEY);
	}

	@Bean
	TopicExchange workloadModelCreatedExchange() {
		// Not declaring auto delete, since queues are bound dynamically so that the exchange might
		// have no queue for a while
		return new TopicExchange(WORKLOAD_MODEL_CREATED_EXCHANGE_NAME, false, false);
	}

}
