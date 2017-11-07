package org.continuity.wessbas.model;

import org.continuity.wessbas.model.config.RabbitMqConfig;
import org.continuity.wessbas.model.instance.WessbasDslInstance;
import org.continuity.wessbas.model.storage.SimpleModelStorage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class BehaviorAmqpHandler {

	@Autowired
	private ModelCreatedSender sender;

	@RabbitListener(queues = RabbitMqConfig.BEHAVIOR_EXTRACTED_QUEUE_NAME)
	public void onBehaviorExtracted(Object message) {
		System.out.println("Received message: " + message);

		String id = SimpleModelStorage.instance().put(WessbasDslInstance.DVDSTORE_PARSED.get());
		sender.sendCreated(id);
	}

}
