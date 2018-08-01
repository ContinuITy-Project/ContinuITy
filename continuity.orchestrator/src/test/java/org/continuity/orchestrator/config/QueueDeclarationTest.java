package org.continuity.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.EVENT_FINISHED_QUEUE_NAME).as("The defined queue name should be equal to the derived one.")
				.isEqualTo(AmqpApi.Global.EVENT_FINISHED.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
