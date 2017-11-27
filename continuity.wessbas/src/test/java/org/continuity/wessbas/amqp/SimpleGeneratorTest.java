//package org.continuity.wessbas.amqp;
//
//import java.util.HashMap;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.TopicExchange;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitAdmin;
//import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
//import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
//import org.springframework.amqp.support.converter.DefaultClassMapper;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
///**
// * @author Henning Schulz
// *
// */
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class SimpleGeneratorTest {
//
//	private ModelCreatedHandlingStub receivingStub;
//
//	@Autowired
//	private ConnectionFactory connectionFactory;
//
//	@Autowired
//	private AmqpTemplate amqpTemplate;
//
//	@Before
//	public void setupReceivingQueue() {
//		this.receivingStub = new ModelCreatedHandlingStub();
//
//		// set up the queue, exchange, binding on the broker
//		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
//		Queue queue = new Queue(ModelGeneratorTestConfig.MODEL_CREATED_QUEUE_NAME, false);
//		admin.declareQueue(queue);
//		TopicExchange exchange = new TopicExchange(ModelGeneratorTestConfig.MODEL_CREATED_EXCHANGE_NAME, false, true);
//		admin.declareExchange(exchange);
//		admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("*"));
//
//		// set up the listener and container
//		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
//
//		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
//		DefaultClassMapper typeMapper = new DefaultClassMapper();
//		typeMapper.setDefaultType(HashMap.class);
//		typeMapper.setTrustedPackages("*");
//		converter.setClassMapper(typeMapper);
//		MessageListenerAdapter adapter = new MessageListenerAdapter(receivingStub, converter);
//		adapter.setMessageConverter(converter);
//		container.setMessageListener(adapter);
//		container.setQueueNames(ModelGeneratorTestConfig.MODEL_CREATED_QUEUE_NAME);
//		container.start();
//	}
//
//	@Test
//	public void test() throws InterruptedException {
//		amqpTemplate.convertAndSend(ModelGeneratorTestConfig.BEHAVIOR_EXTRACTED_EXCHANGE_NAME, "", "Hi there");
//		// assertEquals("Received workload model should be equal to sent one!",
//		// ModelGeneratorTestConfig.BEHAVIOR_EXTRACTED_EXCHANGE_NAME,
//		// receivingStub.getReceivedWorkloadModel());
//
//		Thread.sleep(1000);
//
//		receivingStub.getReceivedWorkloadModel();
//	}
//
//	@After
//	public void shutdownReceivingQueue() {
//		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
//		admin.deleteQueue(ModelGeneratorTestConfig.MODEL_CREATED_QUEUE_NAME);
//	}
//
//}
