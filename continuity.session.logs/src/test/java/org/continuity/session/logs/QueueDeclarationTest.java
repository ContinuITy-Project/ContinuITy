package org.continuity.session.logs;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.session.logs.config.RabbitMqConfig;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.TASK_CREATE_QUEUE_NAME).as("The defined queue name should be equal to the derived one.")
				.isEqualTo(AmqpApi.SessionLogs.TASK_CREATE.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
