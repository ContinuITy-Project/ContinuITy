package org.continuity.wessbas.model;

/**
 * @author Henning Schulz
 *
 */
public class ModelGeneratorTestConfig {

	public static final String MODEL_CREATED_EXCHANGE_NAME = "model-created";

	public static final String MODEL_CREATED_QUEUE_NAME = "model-created-test";

	public static final String BEHAVIOR_EXTRACTED_EXCHANGE_NAME = "wessbas-behavior-extracted";

	// // Receiver
	//
	// @Bean
	// public ModelCreatedHandlingStub receivingStub() {
	// return new ModelCreatedHandlingStub();
	// }
	//
	// // Output exchange
	//
	// @Bean
	// public FanoutExchange behaviorExtractedExchange() {
	// return new FanoutExchange(BEHAVIOR_EXTRACTED_EXCHANGE_NAME);
	// }
	//
	// @Bean
	// public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
	// final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
	// rabbitTemplate.setMessageConverter(jsonMessageConverter());
	// return rabbitTemplate;
	// }
	//
	// // Input queue
	//
	// @Bean
	// Queue modelCreatedQueue() {
	// return new Queue(MODEL_CREATED_QUEUE_NAME, false);
	// }
	//
	// @Bean
	// FanoutExchange modelCreatedExchange() {
	// return new FanoutExchange(MODEL_CREATED_EXCHANGE_NAME);
	// }
	//
	// @Bean
	// Binding binding() {
	// return BindingBuilder.bind(modelCreatedQueue()).to(modelCreatedExchange());
	// }
	//
	// @Bean
	// public MessageConverter jsonMessageConverter() {
	// return new Jackson2JsonMessageConverter();
	// }
	//
	// @Bean
	// public SimpleRabbitListenerContainerFactory containerFactory(ConnectionFactory
	// connectionFactory, SimpleRabbitListenerContainerFactoryConfigurer configurer) {
	// SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
	// configurer.configure(factory, connectionFactory);
	// factory.setMessageConverter(jsonMessageConverter());
	// return factory;
	// }

}
