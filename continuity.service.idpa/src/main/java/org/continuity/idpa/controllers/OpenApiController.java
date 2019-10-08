package org.continuity.idpa.controllers;

import static org.continuity.api.rest.RestApi.Idpa.OpenApi.ROOT;
import static org.continuity.api.rest.RestApi.Idpa.OpenApi.Paths.UPDATE_FROM_JSON;
import static org.continuity.api.rest.RestApi.Idpa.OpenApi.Paths.UPDATE_FROM_URL;

import java.net.MalformedURLException;
import java.net.URL;

import org.continuity.commons.idpa.OpenApiToIdpaTransformer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.application.Application;
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

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Offers a REST API for updating application models from Open API specifications.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class OpenApiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiController.class);

	private final OpenApiToIdpaTransformer transformer;

	private final ApplicationController systemModelController;

	@Autowired
	public OpenApiController(ApplicationController systemModelController) {
		this.transformer = new OpenApiToIdpaTransformer();
		this.systemModelController = systemModelController;
	}

	/**
	 * Parses the specified Open API JSON, transforms it to a {@link Application} and updates the
	 * already stored application model.
	 *
	 * @param aid
	 *            App-id of the application model.
	 * @param version
	 *            Open API version (currently, only 2.0 is supported).
	 * @param json
	 *            Open API specification (JSON).
	 * @return
	 */
	@RequestMapping(path = UPDATE_FROM_JSON, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateFromJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String version, @RequestBody JsonNode json) {
		Swagger swagger;

		if ("2.0".equals(version)) {
			swagger = new SwaggerParser().read(json);
		} else {
			return ResponseEntity.badRequest().body("Currently, only version 2.0 is supported!");
		}

		Application system = transformer.transform(swagger);

		return systemModelController.updateApplication(aid, system);
	}

	/**
	 * Reads the Open API specification from the specified URL, transforms it to a
	 * {@link Application} and updates the already stored application model.
	 *
	 * @param aid
	 *            App-id of the application model.
	 * @param version
	 *            Open API version (currently, only 2.0 is supported).
	 * @param url
	 *            URL where the Open API specification can be retrieved from.
	 * @return
	 */
	@RequestMapping(path = UPDATE_FROM_URL, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateFromUrl(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String version, @RequestBody String url) {
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

		Application system = transformer.transform(swagger);

		return systemModelController.updateApplication(aid, system);
	}

}
