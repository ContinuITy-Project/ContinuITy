package org.continuity.idpa.annotation.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.api.amqp.AmqpApi;
import org.junit.Test;

public class QueueDeclarationTest {

	@Test
	public void test() {
		assertThat(RabbitMqConfig.WORKLOAD_MODEL_CREATED_QUEUE_NAME).as("The defined queue name sould be equal to the derived one.")
				.isEqualTo(AmqpApi.WorkloadModel.EVENT_CREATED.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
		assertThat(RabbitMqConfig.IDPA_APPLICATION_CHANGED_QUEUE_NAME).as("The defined queue name sould be equal to the derived one.")
				.isEqualTo(AmqpApi.IdpaApplication.EVENT_CHANGED.deriveQueueName(RabbitMqConfig.SERVICE_NAME));
	}

}
