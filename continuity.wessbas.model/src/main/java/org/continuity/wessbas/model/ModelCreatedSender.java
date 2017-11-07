package org.continuity.wessbas.model;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.continuity.wessbas.model.config.RabbitMqConfig;
import org.continuity.wessbas.model.data.WorkloadModelLink;
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
		// ObjectMapper mapper = new ObjectMapper();
		//
		// EMFModule module = new EMFModule();
		// module.configure(EMFModule.Feature.OPTION_SERIALIZE_TYPE, false);
		// mapper.registerModule(module);
		//
		// JsonNode workloadModelJson = mapper.valueToTree(workloadModel);
		// ObjectNode root = mapper.createObjectNode();
		// root.set("type", new TextNode("wessbas"));
		// root.set("workload-model", workloadModelJson);
		// amqpTemplate.convertAndSend(ModelGeneratorConfig.MODEL_CREATED_EXCHANGE_NAME, "", root);

		String address;
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			address = "UNKNOWN";
		}

		String link = address + ":TODO/wessbas/model/" + id;
		amqpTemplate.convertAndSend(RabbitMqConfig.MODEL_CREATED_EXCHANGE_NAME, "wessbas", new WorkloadModelLink(link));
	}

}
