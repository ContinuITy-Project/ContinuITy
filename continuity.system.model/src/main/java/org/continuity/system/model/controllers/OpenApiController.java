package org.continuity.system.model.controllers;

import java.net.MalformedURLException;
import java.net.URL;

import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.system.model.openapi.OpenApiToContinuityTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

/**
 * Offers a REST API for updating system models from Open API specifications.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("openapi")
public class OpenApiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiController.class);

	private final OpenApiToContinuityTransformer transformer;

	private final SystemModelController systemModelController;

	@Autowired
	public OpenApiController(SystemModelController systemModelController) {
		this.transformer = new OpenApiToContinuityTransformer();
		this.systemModelController = systemModelController;
	}

	/**
	 * Parses the specified Open API JSON, transforms it to a {@link SystemModel} and updates the
	 * already stored system model.
	 *
	 * @param tag
	 *            Tag of the system model.
	 * @param version
	 *            Open API version (currently, only 2.0 is supported).
	 * @param json
	 *            Open API specification (JSON).
	 * @return
	 */
	@RequestMapping(path = "{tag}/{version}/json/", method = RequestMethod.POST)
	public ResponseEntity<String> updateFromJson(@PathVariable String tag, @PathVariable String version, @RequestBody JsonNode json) {
		Swagger swagger;

		if ("2.0".equals(version)) {
			swagger = new SwaggerParser().read(json);
		} else {
			return ResponseEntity.badRequest().body("Currently, only version 2.0 is supported!");
		}

		SystemModel system = transformer.transform(swagger);

		return systemModelController.updateSystemModel(tag, system);
	}

	/**
	 * Reads the Open API specification from the specified URL, transforms it to a
	 * {@link SystemModel} and updates the already stored system model.
	 *
	 * @param tag
	 *            Tag of the system model.
	 * @param version
	 *            Open API version (currently, only 2.0 is supported).
	 * @param url
	 *            URL where the Open API specification can be retrieved from.
	 * @return
	 */
	@RequestMapping(path = "{tag}/{version}/url/", method = RequestMethod.POST)
	public ResponseEntity<String> updateFromUrl(@PathVariable String tag, @PathVariable String version, @RequestBody String url) {
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			LOGGER.error("Received malformed URL: {}!", url);
			LOGGER.error("Exception:", e);
			return ResponseEntity.badRequest().body("Malformed URL: " + url);
		}

		Swagger swagger;

		if ("2.0".equals(version)) {
			swagger = new SwaggerParser().read(url);
		} else {
			return ResponseEntity.badRequest().body("Currently, only version 2.0 is supported!");
		}

		SystemModel system = transformer.transform(swagger);

		return systemModelController.updateSystemModel(tag, system);
	}

}
