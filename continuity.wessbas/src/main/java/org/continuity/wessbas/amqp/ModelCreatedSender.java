package org.continuity.wessbas.amqp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.continuity.wessbas.config.RabbitMqConfig;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class ModelCreatedSender {

	@Autowired
	private AmqpTemplate amqpTemplate;

	public void sendCreated(String id) {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			hostname = "UNKNOWN";
		}

		String base = hostname + "/wessbas";
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas", new WorkloadModelPack(base, "/model/" + id, "/annotation/system/" + id, "/annotation/annotation/" + id));
	}

}
