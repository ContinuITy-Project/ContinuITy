package org.continuity.cobra.config;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMqConfig {

	public static final String SERVICE_NAME = "cobra";

	public static final String TASK_CREATE_QUEUE_NAME = "continuity.cobra.task.global.create";

	public static final String TASK_CREATE_ROUTING_KEY = AmqpApi.Global.TASK_CREATE.formatRoutingKey().ofGenericTarget(SERVICE_NAME);

	public static final String EVENT_CONFIG_AVAILABLE_NAME = "continuity.cobra.event.orchestrator.configavailable";

	public static final String TASK_PROCESS_TRACES_QUEUE_NAME = "continuity.cobra.task.cobra.process_traces";

	public static final String TASK_PROCESS_TRACES_ROUTING_KEY = "#";

	public static final String HEADER_FINISH = "continuity.finish";

	public static final String EVENT_CLUSTINATOR_FINISHED_QUEUE_NAME = "continuity.cobra.event.clustinator.finished";

	public static final String EVENT_CLUSTINATOR_FINISHED_ROUTING_KEY = "#";

	public static final String DEAD_LETTER_QUEUE_NAME = AmqpApi.DEAD_LETTER_EXCHANGE.deriveQueueName(SERVICE_NAME);

	// General

	@Bean
	MessageConverter jsonMessageConverter(ObjectMapper mapper) {
		return new Jackson2JsonMessageConverter(mapper);
	}

	@Bean
	MessagePostProcessor typeRemovingProcessor() {
		return m -> {
			m.getMessageProperties().getHeaders().remove("__TypeId__");
			return m;
		};
	}

	@Bean
	AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(converter);
		rabbitTemplate.setBeforePublishPostProcessors(typeRemovingProcessor());

		return rabbitTemplate;
	}

	@Bean
	SimpleRabbitListenerContainerFactory containerFactory(ConnectionFactory connectionFactory, MessageConverter converter, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(converter);
		factory.setAfterReceivePostProcessors(typeRemovingProcessor());
		return factory;
	}

	@Bean
	TopicExchange taskCreateExchange() {
		return AmqpApi.Global.TASK_CREATE.create();
	}

	@Bean
	Queue taskCreateQueue() {
		return QueueBuilder.nonDurable(TASK_CREATE_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.Global.EVENT_FAILED.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding taskCreateBinding() {
		return BindingBuilder.bind(taskCreateQueue()).to(taskCreateExchange()).with(TASK_CREATE_ROUTING_KEY);
	}

	@Bean
	TopicExchange eventFinishedExchange() {
		return AmqpApi.Global.EVENT_FINISHED.create();
	}

	@Bean
	TopicExchange eventFailedExchange() {
		return AmqpApi.Global.EVENT_FAILED.create();
	}

	@Bean
	TopicExchange eventConfigAvailableExchange() {
		return AmqpApi.Orchestrator.EVENT_CONFIG_AVAILABLE.create();
	}

	@Bean
	Queue eventConfigAvailableQueue() {
		return QueueBuilder.nonDurable(EVENT_CONFIG_AVAILABLE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding eventConfigAvailableBinding() {
		return BindingBuilder.bind(eventConfigAvailableQueue()).to(eventConfigAvailableExchange()).with(CobraConfiguration.SERVICE);
	}

	@Bean
	TopicExchange taskProcessTracesExchange() {
		return AmqpApi.Cobra.TASK_PROCESS_TRACES.create();
	}

	@Bean
	Queue taskProcessTracesQueue() {
		return QueueBuilder.nonDurable(TASK_PROCESS_TRACES_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding taskProcessTracesBinding() {
		return BindingBuilder.bind(taskProcessTracesQueue()).to(taskProcessTracesExchange()).with(TASK_PROCESS_TRACES_ROUTING_KEY);
	}

	@Bean
	TopicExchange taskClustinatorClusterExchange() {
		return AmqpApi.Cobra.Clustinator.TASK_CLUSTER.create();
	}

	@Bean
	TopicExchange eventClustinatorFinishedExchange() {
		return AmqpApi.Cobra.Clustinator.EVENT_FINISHED.create();
	}

	@Bean
	Queue eventClustinatorFinishedQueue() {
		return QueueBuilder.nonDurable(EVENT_CLUSTINATOR_FINISHED_QUEUE_NAME).withArgument(AmqpApi.DEAD_LETTER_EXCHANGE_KEY, AmqpApi.DEAD_LETTER_EXCHANGE.name())
				.withArgument(AmqpApi.DEAD_LETTER_ROUTING_KEY_KEY, SERVICE_NAME).build();
	}

	@Bean
	Binding eventClustinatorFinishedBinding() {
		return BindingBuilder.bind(eventClustinatorFinishedQueue()).to(eventClustinatorFinishedExchange()).with(EVENT_CLUSTINATOR_FINISHED_ROUTING_KEY);
	}

	@Bean
	TopicExchange taskClustinatorKnnDistanceExchange() {
		return AmqpApi.Cobra.Clustinator.TASK_KNN_DISTANCE.create();
	}

	@Bean
	TopicExchange eventClustinatorImagegeneratedExchange() {
		return AmqpApi.Cobra.Clustinator.EVENT_IMAGEGENERATED.create();
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
