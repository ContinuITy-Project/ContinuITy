package org.continuity.workload.annotation.amqp;

import org.continuity.workload.annotation.config.RabbitMqConfig;
import org.continuity.workload.annotation.entities.WorkloadModelLink;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class AnnotationAmpqHandler {

	@RabbitListener(queues = RabbitMqConfig.MODEL_CREATED_QUEUE_NAME)
	public void onModelCreated(WorkloadModelLink link) {
		System.out.println("Received link: " + link);
	}

}
