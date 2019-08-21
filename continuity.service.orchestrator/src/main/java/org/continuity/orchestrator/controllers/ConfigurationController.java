package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Configuration.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Configuration.Paths.GET;
import static org.continuity.api.rest.RestApi.Orchestrator.Configuration.Paths.GET_ALL;
import static org.continuity.api.rest.RestApi.Orchestrator.Configuration.Paths.POST;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.ServiceConfiguration;
import org.continuity.api.rest.RestApi.Orchestrator.Configuration;
import org.continuity.idpa.AppId;
import org.continuity.orchestrator.storage.ConfigurationStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controls the configurations of all services.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class ConfigurationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);

	@Autowired
	private ConfigurationStorage storage;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RequestMapping(path = POST, method = RequestMethod.POST, consumes = { "application/x-yaml", "application/json" }, produces = "application/x-yaml")
	public ResponseEntity<String> post(@RequestBody ServiceConfiguration config, HttpServletRequest servletRequest)
			throws JsonGenerationException, JsonMappingException, IOException {
		storage.put(config);

		LOGGER.info("Stored a new configuration for service {} and app-id {}.", config.getService(), config.getAppId());

		distribute(config);

		String host = servletRequest.getServerName() + ":" + servletRequest.getServerPort();
		return ResponseEntity.created(URI.create(Configuration.GET.requestUrl(config.getService(), config.getAppId()).withHost(host).get())).body("created");
	}

	@RequestMapping(path = GET, method = RequestMethod.GET, produces = { "application/json", "application/x-yaml" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<ServiceConfiguration> get(@PathVariable String service, @ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam(required = false, defaultValue = "false") boolean init)
			throws JsonParseException, JsonMappingException, IOException {
		ServiceConfiguration config = storage.get(service, aid);

		if (config == null) {
			if (init) {
				LOGGER.info("Could not find configuration for service {} and app-id {}. Returning default one.", service, aid);

				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(storage.getDefault(service, aid));
			} else {
				LOGGER.warn("Could not find configuration for service {} and app-id {} without fallback!", service, aid);

				return ResponseEntity.notFound().build();
			}
		} else {
			LOGGER.info("Found and returned configuration for service {} and app-id {}.", service, aid);

			return ResponseEntity.ok(config);
		}
	}

	@RequestMapping(path = GET_ALL, method = RequestMethod.GET, produces = { "application/json", "application/x-yaml" })
	public ResponseEntity<List<ServiceConfiguration>> getAll(@PathVariable String service) throws JsonParseException, JsonMappingException, IOException {
		List<ServiceConfiguration> configs = storage.get(service);
		return ResponseEntity.ok(configs);
	}

	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) {
		LOGGER.info("Initializing all services with the existing configurations...");

		try {
			storage.getAll().forEach(this::distribute);
		} catch (IOException e) {
			LOGGER.error("Could not read all configurations!", e);
		}

		LOGGER.info("Initialization done.");
	}

	private void distribute(ServiceConfiguration config) {
		LOGGER.info("Sending new configuration for app-id {} to {}.", config.getAppId(), config.getService());
		amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_CONFIG_AVAILABLE.name(), AmqpApi.Orchestrator.EVENT_CONFIG_AVAILABLE.formatRoutingKey().of(config.getService()), config);
	}

}
