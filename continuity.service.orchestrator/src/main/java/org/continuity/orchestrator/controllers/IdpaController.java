package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.GET_ANNOTATION;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.GET_APPLICATION;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.GET_BROKEN;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.UPDATE_ANNOTATION;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.UPDATE_APPLICATION;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.UPDATE_APP_FROM_OPEN_API_JSON;
import static org.continuity.api.rest.RestApi.Orchestrator.Idpa.Paths.UPDATE_APP_FROM_OPEN_API_URL;

import java.io.IOException;
import java.util.Map;

import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi.Idpa;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.AppId;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

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

	/**
	 * Gets the application model for the specified app-id.
	 *
	 * @param aid
	 *            The app-id of the application.
	 * @param timestamp
	 *            The timestamp for which the application is requested.
	 * @return The application model.
	 */
	@RequestMapping(path = GET_APPLICATION, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<Application> getApplication(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam(required = false) String timestamp) {
		RequestBuilder req = Idpa.Application.GET.requestUrl(aid);

		if (timestamp != null) {
			req = req.withQuery("timestamp", timestamp);
		}

		return restTemplate.getForEntity(req.get(), Application.class);
	}

	/**
	 * Gets the annotation for the specified app-id.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @param timestamp
	 *            The timestamp for which the annotation is requested.
	 * @return The annotation.
	 */
	@RequestMapping(path = GET_ANNOTATION, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<ApplicationAnnotation> getAnnotation(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam(required = false) String timestamp) {
		try {
			RequestBuilder req = Idpa.Annotation.GET.requestUrl(aid);

			if (timestamp != null) {
				req = req.withQuery("timestamp", timestamp);
			}

			return restTemplate.getForEntity(req.get(), ApplicationAnnotation.class);
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

				LOGGER.warn("Tried to get the annotation for app-id {}, but got a {} ({}) response: \"{}\". Trying the redirect {}.", aid, e.getStatusCode(), e.getStatusCode().getReasonPhrase(),
						message,
						redirect);
				return restTemplate.getForEntity(WebUtils.addProtocolIfMissing(redirect), ApplicationAnnotation.class);
			}

			return ResponseEntity.status(e.getStatusCode()).build();
		}
	}

	/**
	 * Updates the application model for the specified app-id.
	 *
	 * @param aid
	 *            The app-id of the application.
	 * @param application
	 *            The application model.
	 */
	@RequestMapping(path = UPDATE_APPLICATION, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateApplication(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestBody Application application) {
		try {
			return restTemplate.postForEntity(Idpa.Application.UPDATE.requestUrl(aid).get(), application, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the system model with app-id {} resulted in a {} - {} response!", aid, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Updates the application model for the specified app-id using the passed Open API JSON.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @param version
	 *            The version of the Open API specification.
	 * @param json
	 *            The Open API JSON.
	 * @return A report about the changes.
	 */
	@RequestMapping(path = UPDATE_APP_FROM_OPEN_API_JSON, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateAppFromOpenApiJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable("version") String version, @RequestBody JsonNode json) {
		try {
			return restTemplate.postForEntity(Idpa.OpenApi.UPDATE_FROM_JSON.requestUrl(aid, version).get(), json, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the system model with app-id {} resulted in a {} - {} response!", aid, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Updates the system model for the specified app-id fetching the Open API specification from
	 * the specified URL.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @param version
	 *            The version of the Open API specification.
	 * @param url
	 *            The URL to retrieve the Open API specification from.
	 * @return A report about the changes.
	 */
	@RequestMapping(path = UPDATE_APP_FROM_OPEN_API_URL, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateAppFromOpenApiUrl(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable("version") String version, @RequestBody String url) {
		try {
			return restTemplate.postForEntity(Idpa.OpenApi.UPDATE_FROM_URL.requestUrl(aid, version).get(), url, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the system model with app-id {} resulted in a {} - {} response!", aid, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Updates the annotation for the specified app-id.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @param annotation
	 *            The annotation.
	 */
	@RequestMapping(path = UPDATE_ANNOTATION, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateAnnotation(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam String timestamp, @RequestBody ApplicationAnnotation annotation) {
		try {
			return restTemplate.postForEntity(Idpa.Annotation.UPDATE.requestUrl(aid).withQuery("timestamp", timestamp).get(), annotation, String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Updating the annotation with app-id {} resulted in a {} - {} response!", aid, e.getStatusCode(), e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

	/**
	 * Returns the timestamps of all annotations that are broken due to a certain application.
	 *
	 * @param aid
	 *            The app-id.
	 * @param timestamp
	 *            The timestamp of the application.
	 * @return A list with the timestamps of all broken annotations - formatted as JSON string.
	 */
	@RequestMapping(path = GET_BROKEN, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getBroken(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam String timestamp) {
		try {
			return restTemplate.getForEntity(Idpa.Annotation.GET_BROKEN.requestUrl(aid).withQuery("timestamp", timestamp).get(), String.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.warn("Getting the broken states for the application with app-id {} and timestamp {} resulted in a {} - {} response!", aid, timestamp, e.getStatusCode(),
					e.getStatusCode().getReasonPhrase());
			return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
		}
	}

}
