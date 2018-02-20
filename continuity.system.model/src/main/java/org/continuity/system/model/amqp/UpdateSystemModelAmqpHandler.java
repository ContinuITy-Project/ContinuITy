package org.continuity.system.model.amqp;

import java.util.EnumSet;

import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.utils.WebUtils;
import org.continuity.system.model.config.RabbitMqConfig;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemChangeType;
import org.continuity.system.model.entities.SystemModelLink;
import org.continuity.system.model.entities.WorkloadModelLink;
import org.continuity.system.model.repository.SystemModelRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * AMQP handler for the {@link RabbitMqConfig#MODEL_CREATED_QUEUE_NAME} queue.
 *
 * @author Henning Schulz
 *
 */
@Component
public class UpdateSystemModelAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSystemModelAmqpHandler.class);

	private final SystemModelRepositoryManager repositoryManager;

	private final RestTemplate restTemplate;

	private final AmqpTemplate amqpTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	@Autowired
	public UpdateSystemModelAmqpHandler(SystemModelRepositoryManager repositoryManager, RestTemplate restTemplate, AmqpTemplate amqpTemplate) {
		this.repositoryManager = repositoryManager;
		this.restTemplate = restTemplate;
		this.amqpTemplate = amqpTemplate;
	}

	/**
	 * Listens to the {@link RabbitMqConfig#MODEL_CREATED_QUEUE_NAME} queue and saves the incoming
	 * system model if it is different to the already stored ones.
	 *
	 * @param link
	 *            Containing all links around the created workload model.
	 */
	@RabbitListener(queues = RabbitMqConfig.MODEL_CREATED_QUEUE_NAME)
	public void onModelCreated(WorkloadModelLink link) {
		LOGGER.info("Received workload model link: {}", link);

		if ("INVALID".equals(link.getSystemModelLink())) {
			LOGGER.error("Received invalid system model link: {}", link);
			return;
		}

		ResponseEntity<SystemModel> systemResponse;
		try {
			systemResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(link.getSystemModelLink()), SystemModel.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not retrieve the system model from {}. Got response code {}!", link.getSystemModelLink(), e.getStatusCode());
			LOGGER.error("Exception:", e);
			return;
		}

		SystemModel systemModel = systemResponse.getBody();

		// A workload model may only contain parts of all available interfaces, possible parameters
		// and possible properties (e.g., headers).
		SystemChangeReport report = repositoryManager.saveOrUpdate(link.getTag(), systemModel,
				EnumSet.of(SystemChangeType.INTERFACE_REMOVED, SystemChangeType.INTERFACE_CHANGED, SystemChangeType.PARAMETER_REMOVED));

		if (report.changed()) {
			try {
				amqpTemplate.convertAndSend(RabbitMqConfig.SYSTEM_MODEL_CHANGED_EXCHANGE_NAME, link.getTag(), new SystemModelLink(applicationName, link.getTag(), report.getBeforeChange()));
			} catch (AmqpException e) {
				LOGGER.error("Could not send the system model with tag {} to the {} exchange!", link.getTag(), RabbitMqConfig.SYSTEM_MODEL_CHANGED_EXCHANGE_NAME);
				LOGGER.error("Exception:", e);
			}
		} else {
			LOGGER.info("The new system model from link {} did not change.", link);
		}
	}
}
