package org.continuity.frontend.controllers;

import static org.continuity.api.rest.RestApi.Frontend.Idpa.ROOT;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.GET_ANNOTATION;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.GET_APPLICATION;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.REPORT;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.UPDATE_ANNOTATION;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.UPDATE_APPLICATION;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.UPDATE_APP_FROM_OPEN_API_JSON;
import static org.continuity.api.rest.RestApi.Frontend.Idpa.Paths.UPDATE_APP_FROM_OPEN_API_URL;

import java.io.IOException;
import java.util.Map;

import org.continuity.api.rest.RestApi.IdpaAnnotation;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.utils.WebUtils;
import org.continuity.frontend.config.RabbitMqConfig;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class IdpaController {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdpaController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	/**
	 * Gets the application model for the specified tag.
	 *
	 * @param tag
	 *            The tag of the application.
	 * @return The application model.
	 */
	@RequestMapping(path = GET_APPLICATION, method = RequestMethod.GET)
	public ResponseEntity<Application> getApplication(@PathVariable("tag") String tag) {
		return restTemplate.getForEntity(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);
	}

	/**
	 * Gets the annotation for the specified tag.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return The annotation.
	 */
	@RequestMapping(path = GET_ANNOTATION, method = RequestMethod.GET)
	public ResponseEntity<ApplicationAnnotation> getAnnotation(@PathVariable("tag") String tag) {
		try {
			return restTemplate.getForEntity(IdpaAnnotation.Annotation.GET.requestUrl(tag).get(), ApplicationAnnotation.class);
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
				return restTemplate.getForEntity(WebUtils.addProtocolIfMissing(redirect), ApplicationAnnotation.class);
			}

			return ResponseEntity.status(e.getStatusCode()).build();
		}
	}

	/**
	 * Updates the application model for the specified tag.
	 *
	 * @param tag
	 *            The tag of the application.
	 * @param application
	 *            The application model.
	 */
	@RequestMapping(path = UPDATE_APPLICATION, method = RequestMethod.POST)
	public ResponseEntity<String> updateApplication(@PathVariable("tag") String tag, @RequestBody Application application) {
		try {
			return restTemplate.postForEntity(IdpaApplication.Application.UPDATE.requestUrl(tag).get(), application, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the system model with tag {} resulted in a {} - {} response!", tag, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Updates the application model for the specified tag using the passed Open API JSON.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param version
	 *            The version of the Open API specification.
	 * @param json
	 *            The Open API JSON.
	 * @return A report about the changes.
	 */
	@RequestMapping(path = UPDATE_APP_FROM_OPEN_API_JSON, method = RequestMethod.POST)
	public ResponseEntity<String> updateAppFromOpenApiJson(@PathVariable("tag") String tag, @PathVariable("version") String version, @RequestBody JsonNode json) {
		try {
			return restTemplate.postForEntity(IdpaApplication.OpenApi.UPDATE_FROM_JSON.requestUrl(tag, version).get(), json, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the system model with tag {} resulted in a {} - {} response!", tag, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Updates the system model for the specified tag fetching the Open API specification from the
	 * specified URL.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param version
	 *            The version of the Open API specification.
	 * @param url
	 *            The URL to retrieve the Open API specification from.
	 * @return A report about the changes.
	 */
	@RequestMapping(path = UPDATE_APP_FROM_OPEN_API_URL, method = RequestMethod.POST)
	public ResponseEntity<String> updateAppFromOpenApiUrl(@PathVariable("tag") String tag, @PathVariable("version") String version, @RequestBody String url) {
		try {
			return restTemplate.postForEntity(IdpaApplication.OpenApi.UPDATE_FROM_URL.requestUrl(tag, version).get(), url, String.class);
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
	@RequestMapping(path = UPDATE_ANNOTATION, method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody ApplicationAnnotation annotation) {
		try {
			return restTemplate.postForEntity(IdpaAnnotation.Annotation.UPDATE.requestUrl(tag).get(), annotation, String.class);
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
	@RequestMapping(path = REPORT, method = RequestMethod.GET)
	public ResponseEntity<?> getAnnotationReport(@RequestParam(value = "timeout", required = true) long timeout) {
		Map<?, ?> report = amqpTemplate.receiveAndConvert(RabbitMqConfig.IDPA_ANNOTATION_MESSAGE_AVAILABLE_QUEUE_NAME, timeout, ParameterizedTypeReference.forType(Map.class));

		if (report != null) {
			return ResponseEntity.ok(report);
		} else {
			return ResponseEntity.noContent().build();
		}
	}

}
