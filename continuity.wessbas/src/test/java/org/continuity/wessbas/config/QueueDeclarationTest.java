package org.continuity.wessbas.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.MONITORING_DATA_AVAILABLE_QUEUE_NAME).as("The defined queue name sould be equal to the derived one.")
				.isEqualTo(AmqpApi.Frontend.DATA_AVAILABLE.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
