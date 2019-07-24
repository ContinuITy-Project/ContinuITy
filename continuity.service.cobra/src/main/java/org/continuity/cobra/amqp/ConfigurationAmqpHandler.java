package org.continuity.cobra.amqp;

import java.util.Arrays;

import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.session.logs.CobraConfiguration;
import org.continuity.api.rest.RestApi;
import org.continuity.cobra.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ConfigurationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAmqpHandler.class);

	@Autowired
	private ConfigurationProvider<CobraConfiguration> provider;

	@Autowired
	private RestTemplate restTemplate;

	@RabbitListener(queues = RabbitMqConfig.EVENT_CONFIG_AVAILABLE_NAME)
	public void onConfigurationAvailable(CobraConfiguration config) {
		LOGGER.info("Received new configuration for app-id {}.", config.getAppId());

		provider.refresh(config);
	}

	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) {
		LOGGER.info("Trying to retrieve the initial configuration from the orchestrator...");

		CobraConfiguration[] configs;
		try {
			configs = restTemplate.getForObject(RestApi.Orchestrator.Configuration.GET_ALL.requestUrl(CobraConfiguration.SERVICE).get(), CobraConfiguration[].class);
		} catch (RestClientException | IllegalStateException e) {
			LOGGER.warn("Could not retrieve the initial configuration - {}: {}! This can be normal during startup. Starting with empty configurations.", e.getClass().getName(), e.getMessage());
			configs = new CobraConfiguration[] {};
		}

		provider.init(Arrays.asList(configs));
	}

}
