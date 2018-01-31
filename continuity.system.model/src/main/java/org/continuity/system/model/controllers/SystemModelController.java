package org.continuity.system.model.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.format.CommonFormats;
import org.continuity.system.model.config.RabbitMqConfig;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemModelLink;
import org.continuity.system.model.repository.SystemModelRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offers a REST API for controlling the stored system models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("system")
public class SystemModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemModelController.class);

	private static final DateFormat DATE_FORMAT = CommonFormats.DATE_FORMAT;

	private final AmqpTemplate amqpTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	private final SystemModelRepositoryManager manager;

	@Autowired
	public SystemModelController(SystemModelRepositoryManager manager, AmqpTemplate amqpTemplate) {
		this.manager = manager;
		this.amqpTemplate = amqpTemplate;
	}

	/**
	 * Retrieves the current system model.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return The current model.
	 */
	@RequestMapping(path = "/{tag}", method = RequestMethod.GET)
	public ResponseEntity<SystemModel> getSystemModel(@PathVariable String tag) {
		try {
			return ResponseEntity.ok(manager.read(tag));
		} catch (IOException e) {
			LOGGER.error("En exception occured during reading!", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Gets a report holding the difference between the current system model and a date in the past.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param since
	 *            The date to compare against in the format {@link CommonFormats#DATE_FORMAT}.
	 * @return The delta report.
	 */
	@RequestMapping(path = "/{tag}/delta", method = RequestMethod.GET)
	public ResponseEntity<SystemChangeReport> getDeltaSince(@PathVariable String tag, @RequestParam("since") String since) {
		Date date;
		try {
			date = DATE_FORMAT.parse(since);
		} catch (ParseException e) {
			LOGGER.error("Could not parse since date {}.", since);
			LOGGER.error("Exception:", e);
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.ok(manager.getDeltaSince(tag, date));
	}

	/**
	 * Stores a new system model if it differs from existing ones.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param system
	 *            The system model to be stored.
	 * @return A report holding the differences between the passed model and the next older one.
	 * @throws URISyntaxException
	 */
	@RequestMapping(path = "/{tag}", method = RequestMethod.POST)
	public ResponseEntity<String> updateSystemModel(@PathVariable String tag, @RequestBody SystemModel system) throws URISyntaxException {
		SystemChangeReport report = manager.saveOrUpdate(tag, system);

		if (report.changed()) {
			try {
				amqpTemplate.convertAndSend(RabbitMqConfig.SYSTEM_MODEL_CHANGED_EXCHANGE_NAME, tag, new SystemModelLink(applicationName, tag, report.getBeforeChange()));
			} catch (AmqpException e) {
				LOGGER.error("Could not send the system model with tag {} to the {} exchange!", tag, RabbitMqConfig.SYSTEM_MODEL_CHANGED_EXCHANGE_NAME);
				LOGGER.error("Exception:", e);
			}
		}

		return ResponseEntity.ok().body(report.toString());
	}

}
