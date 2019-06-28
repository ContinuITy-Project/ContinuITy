package org.continuity.idpa.controllers;

import static org.continuity.api.rest.RestApi.Idpa.Annotation.ROOT;
import static org.continuity.api.rest.RestApi.Idpa.Annotation.Paths.GET;
import static org.continuity.api.rest.RestApi.Idpa.Annotation.Paths.GET_BROKEN;
import static org.continuity.api.rest.RestApi.Idpa.Annotation.Paths.UPDATE;
import static org.continuity.api.rest.RestApi.Idpa.Annotation.Paths.UPLOAD;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.rest.CustomHeaders;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.continuity.idpa.storage.AnnotationStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class AnnotationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

	@Value("${spring.application.name}")
	private String applicationName;

	private final AnnotationStorageManager storageManager;

	@Autowired
	public AnnotationController(AnnotationStorageManager storageManager) {
		this.storageManager = storageManager;
	}

	/**
	 * Retrieves the latest annotation if it is not broken. If the timestamp is {@code null}, the
	 * latest version will be returned.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @param version
	 *            The timestamp for which the application model is searched (optional).
	 * @return {@link ResponseEntity} holding the annotation. It will hold a header
	 *         {@link CustomHeaders#BROKEN} indicating whether the IDPA is broken.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<?> getAnnotation(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam(required = false) String version) {
		VersionOrTimestamp vot = null;

		if (version != null) {
			try {
				vot = VersionOrTimestamp.fromString(version);
			} catch (ParseException | NumberFormatException e) {
				LOGGER.error("Could not parse version or timestamp {}.", version);
				LOGGER.error("Exception:", e);
				return ResponseEntity.badRequest().build();
			}
		}

		ApplicationAnnotation annotation;

		try {
			if (vot == null) {
				annotation = storageManager.read(aid);
			} else {
				annotation = storageManager.read(aid, vot);
			}
		} catch (IOException e) {
			LOGGER.error("Error during getting annotation with app-id " + aid, e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (annotation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			boolean broken = ((vot == null) && storageManager.isBroken(aid)) || ((vot != null) && storageManager.isBroken(aid, vot));
			return ResponseEntity.ok().header(CustomHeaders.BROKEN, Boolean.toString(broken)).body(annotation);
		}
	}

	/**
	 * Updates the annotation stored with the specified app-id. If the annotation is invalid with
	 * respect to the system model, it is rejected.
	 *
	 * @param aid
	 * @param version
	 * @param annotation
	 * @return
	 */
	@RequestMapping(path = UPDATE, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateAnnotation(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestBody ApplicationAnnotation annotation) {
		AnnotationValidityReport report = null;

		try {
			report = storageManager.saveOrUpdate(aid, annotation);
		} catch (IOException e) {
			LOGGER.error("Error during updating annotation with app-id {}!", aid);
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (report.isBreaking()) {
			return new ResponseEntity<>(report.toString(), HttpStatus.CONFLICT);
		} else {
			return new ResponseEntity<>(report.toString(), HttpStatus.CREATED);
		}
	}

	/**
	 * Updates the annotation stored with the specified app-id. If the annotation is invalid with
	 * respect to the system model, it is rejected.
	 *
	 * @param aid
	 *            app-id of the annotation.
	 * @param version
	 * @param annotation
	 *            The annotation model in YAML format.
	 * @return
	 */
	@RequestMapping(path = UPLOAD, method = RequestMethod.PUT)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> updateAnnotationYaml(@ApiIgnore @PathVariable("app-id") AppId aid,
			@ApiParam(value = "The annotation model in YAML format.", required = true) @RequestBody String annotation) {
		AnnotationValidityReport report = null;

		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		ApplicationAnnotation idpaAnnotation;
		try {
			idpaAnnotation = serializer.readFromYamlString(annotation);
		} catch (IOException e) {
			LOGGER.error("Exception during reading annotation model with app-id {}!", aid);
			LOGGER.error("Exception: ", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			report = storageManager.saveOrUpdate(aid, idpaAnnotation);
		} catch (IOException e) {
			LOGGER.error("Error during updating annotation with app-id {}!", aid);
			LOGGER.error("Exception: ", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (report.isBreaking()) {
			return new ResponseEntity<>(report.toString(), HttpStatus.CONFLICT);
		} else {
			return new ResponseEntity<>(report.toString(), HttpStatus.CREATED);
		}
	}

	/**
	 * Returns the timestamps of all annotations that are broken due to a certain application.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The timestamp of the application.
	 * @return A list with the timestamps of all broken annotations.
	 */
	@RequestMapping(path = GET_BROKEN, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<List<String>> getBroken(@ApiIgnore @PathVariable("app-id") AppId aid, @RequestParam String version) {
		List<String> broken;
		try {
			broken = storageManager.getBrokenForApplication(aid, VersionOrTimestamp.fromString(version)).stream().map(VersionOrTimestamp::toString).collect(Collectors.toList());
		} catch (ParseException e) {
			LOGGER.error("Could not parse timestamp!", e);

			return ResponseEntity.badRequest().body(Arrays.asList("Illegally formatted timestamp: " + version));
		}

		return ResponseEntity.ok(broken);
	}

}
