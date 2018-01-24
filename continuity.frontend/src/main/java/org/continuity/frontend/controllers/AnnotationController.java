package org.continuity.frontend.controllers;

import java.io.IOException;
import java.util.Map;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.utils.WebUtils;
import org.continuity.frontend.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("annotation")
public class AnnotationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	/**
	 * Gets the system model for the specified tag.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @return The system model.
	 */
	@RequestMapping(path = "{tag}/system", method = RequestMethod.GET)
	public ResponseEntity<SystemModel> getSystemModel(@PathVariable("tag") String tag) {
		return restTemplate.getForEntity("http://system-model/system/" + tag, SystemModel.class);
	}

	/**
	 * Gets the annotation for the specified tag.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return The annotation.
	 */
	@RequestMapping(path = "{tag}/annotation", method = RequestMethod.GET)
	public ResponseEntity<SystemAnnotation> getAnnotation(@PathVariable("tag") String tag) {
		try {
			return restTemplate.getForEntity("http://system-annotation/ann/" + tag + "/annotation", SystemAnnotation.class);
		} catch (HttpStatusCodeException e) {
			if (e.getStatusCode() == HttpStatus.LOCKED) {
				ObjectMapper mapper = new ObjectMapper();
				Map<?, ?> body;
				try {
					body = mapper.readValue(e.getResponseBodyAsString(), Map.class);
				} catch (IOException e1) {
					LOGGER.error("Error during deserialization of the JSON body!");
					e1.printStackTrace();
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
				}

				String message = body.get("message").toString();
				String redirect = body.get("redirect").toString();

				LOGGER.warn("Tried to get the annotation for tag {}, but got a {} ({}) response: \"{}\". Trying the redirect {}.", tag, e.getStatusCode(), e.getStatusCode().getReasonPhrase(), message,
						redirect);
				return restTemplate.getForEntity(WebUtils.addProtocolIfMissing(redirect), SystemAnnotation.class);
			}

			return ResponseEntity.status(e.getStatusCode()).build();
		}
	}

	/**
	 * Updates the system model for the specified tag.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param system
	 *            The system model.
	 */
	@RequestMapping(path = "{tag}/system", method = RequestMethod.POST)
	public ResponseEntity<String> updateSystemModel(@PathVariable("tag") String tag, @RequestBody SystemModel system) {
		try {
			return restTemplate.postForEntity("http://system-model/system/" + tag, system, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the system model with tag {} resulted in a {} - {} response!", tag, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Updates the annotation for the specified tag.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param annotation
	 *            The annotation.
	 */
	@RequestMapping(path = "{tag}/annotation", method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody SystemAnnotation annotation) {
		try {
			return restTemplate.postForEntity("http://system-annotation/ann/" + tag + "/annotation", annotation, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the annotation with tag {} resulted in a {} - {} response!", tag, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Retrieves a report from the workload annotation if available.
	 *
	 * @param timeout
	 *            The timeout to wait for messages.
	 * @return A response entity (200) holding a report or 204 (no content) if there is no report.
	 */
	@RequestMapping(path = "report", method = RequestMethod.GET)
	public ResponseEntity<?> getAnnotationReport(@RequestParam(value = "timeout", required = true) long timeout) {
		Map<?, ?> report = amqpTemplate.receiveAndConvert(RabbitMqConfig.WORKLADO_ANNOTATION_MESSAGE_QUEUE_NAME, timeout, ParameterizedTypeReference.forType(Map.class));

		if (report != null) {
			return ResponseEntity.ok(report);
		} else {
			return ResponseEntity.noContent().build();
		}
	}

}
