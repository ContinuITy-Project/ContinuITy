package org.continuity.idpa.application.amqp;

import java.util.EnumSet;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.config.RabbitMqConfig;
import org.continuity.idpa.application.entities.ApplicationModelLink;
import org.continuity.idpa.application.repository.ApplicationModelRepositoryManager;
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

	private final ApplicationModelRepositoryManager repositoryManager;

	private final RestTemplate restTemplate;

	private final AmqpTemplate amqpTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	@Autowired
	public UpdateSystemModelAmqpHandler(ApplicationModelRepositoryManager repositoryManager, RestTemplate restTemplate, AmqpTemplate amqpTemplate) {
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
	@RabbitListener(queues = RabbitMqConfig.WORKLOAD_MODEL_CREATED_QUEUE_NAME)
	public void onModelCreated(LinkExchangeModel link) {
		LOGGER.info("Received workload model link: {}", link);

		if ("INVALID".equals(link.getApplicationLink())) {
			LOGGER.error("Received invalid system model link: {}", link);
			return;
		}

		ResponseEntity<Application> systemResponse;
		try {
			systemResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(link.getApplicationLink()), Application.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not retrieve the system model from {}. Got response code {}!", link.getApplicationLink(), e.getStatusCode());
			LOGGER.error("Exception:", e);
			return;
		}

		Application systemModel = systemResponse.getBody();

		// A workload model may only contain parts of all available interfaces, possible parameters
		// and possible properties (e.g., headers).
		ApplicationChangeReport report = repositoryManager.saveOrUpdate(link.getTag(), systemModel,
				EnumSet.of(ApplicationChangeType.ENDPOINT_REMOVED, ApplicationChangeType.ENDPOINT_CHANGED, ApplicationChangeType.PARAMETER_REMOVED));

		if (report.changed()) {
			try {
				amqpTemplate.convertAndSend(AmqpApi.IdpaApplication.APPLICATION_CHANGED.name(), AmqpApi.IdpaApplication.APPLICATION_CHANGED.formatRoutingKey().of(link.getTag()),
						new ApplicationModelLink(applicationName, link.getTag(), report.getBeforeChange()));
			} catch (AmqpException e) {
				LOGGER.error("Could not send the system model with tag {} to the {} exchange!", link.getTag(), AmqpApi.IdpaApplication.APPLICATION_CHANGED.name());
				LOGGER.error("Exception:", e);
			}
		} else {
			LOGGER.info("The new system model from link {} did not change.", link);
		}
	}
}
