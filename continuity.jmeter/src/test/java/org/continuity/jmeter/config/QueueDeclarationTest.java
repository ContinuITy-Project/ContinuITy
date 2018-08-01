package org.continuity.jmeter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.TASK_CREATE_QUEUE_NAME).as("The defined queue name should be equal to the derived one.")
				.isEqualTo(AmqpApi.LoadTest.TASK_CREATE.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
		assertThat(RabbitMqConfig.TASK_EXECUTE_QUEUE_NAME).as("The defined queue name should be equal to the derived one.")
				.isEqualTo(AmqpApi.LoadTest.TASK_EXECUTE.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
