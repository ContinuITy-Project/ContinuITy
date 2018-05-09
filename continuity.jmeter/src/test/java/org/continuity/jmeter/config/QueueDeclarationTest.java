package org.continuity.jmeter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.LOAD_TEST_EXECUTION_REQUIRED_QUEUE_NAME).as("The defined queue name sould be equal to the derived one.")
				.isEqualTo(AmqpApi.Frontend.LOADTESTEXECUTION_REQUIRED.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
		assertThat(RabbitMqConfig.LOAD_TEST_CREATION_AND_EXECUTION_REQUIRED_QUEUE_NAME).as("The defined queue name sould be equal to the derived one.")
				.isEqualTo(AmqpApi.Frontend.LOADTESTCREATIONANDEXECUTION_REQUIRED.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
