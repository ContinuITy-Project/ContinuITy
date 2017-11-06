package org.continuity.wessbas.model;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class ModelGeneratorService {

	@RabbitListener(queues = ModelGeneratorConfig.QUEUE_NAME)
	public void createWorkloadModel(Object message) {
		System.out.println("Received message: " + message);

	}

}
