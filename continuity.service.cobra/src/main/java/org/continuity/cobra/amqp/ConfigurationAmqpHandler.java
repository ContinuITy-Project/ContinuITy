package org.continuity.cobra.amqp;

import java.util.Arrays;

import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.api.exception.ServiceConfigurationException;
import org.continuity.api.rest.RestApi;
import org.continuity.cobra.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ConfigurationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAmqpHandler.class);

	private static final String LISTENER_ID = ConfigurationAmqpHandler.class.getSimpleName();

	@Autowired
	private ConfigurationProvider<CobraConfiguration> provider;

	@Autowired
	private RestTemplate restTemplate;

	@RabbitListener(queues = RabbitMqConfig.EVENT_CONFIG_AVAILABLE_NAME)
	public void onConfigurationAvailable(CobraConfiguration config) {
		LOGGER.info("Received new configuration for app-id {}.", config.getAppId());
		try {
			provider.refresh(config, LISTENER_ID);
		} catch (ServiceConfigurationException e) {
			LOGGER.error("Could not properly refresh the configuration! Might be in an inconsistent state!", e);
		}
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

		try {
			provider.init(Arrays.asList(configs));
		} catch (ServiceConfigurationException e) {
			LOGGER.error("Could not properly initialize the configurations! Might be in an inconsistent state!", e);
		}

		provider.registerListener(this::backpropagate, LISTENER_ID);
	}

	private void backpropagate(CobraConfiguration config) throws ServiceConfigurationException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<CobraConfiguration> entity = new HttpEntity<>(config, headers);

		try {
			restTemplate.exchange(RestApi.Orchestrator.Configuration.POST.requestUrl().get(), HttpMethod.POST, entity, String.class);
		} catch (HttpStatusCodeException | IllegalStateException e) {
			throw new ServiceConfigurationException("Could not upload updated configuration to the orchestrator!", e);
		}
	}

}
