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

import io.swagger.annotations.ApiParam;

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
	 * @param tag
	 *            The tag of the annotation.
	 * @param version
	 *            The timestamp for which the application model is searched (optional).
	 * @return {@link ResponseEntity} holding the annotation. It will hold a header
	 *         {@link CustomHeaders#BROKEN} indicating whether the IDPA is broken.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<?> getAnnotation(@PathVariable("tag") String tag, @RequestParam(required = false) String version) {
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
				annotation = storageManager.read(tag);
			} else {
				annotation = storageManager.read(tag, vot);
			}
		} catch (IOException e) {
			LOGGER.error("Error during getting annotation with tag " + tag, e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (annotation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			boolean broken = ((vot == null) && storageManager.isBroken(tag)) || ((vot != null) && storageManager.isBroken(tag, vot));
			return ResponseEntity.ok().header(CustomHeaders.BROKEN, Boolean.toString(broken)).body(annotation);
		}
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the system model, it is rejected.
	 *
	 * @param tag
	 * @param version
	 * @param annotation
	 * @return
	 */
	@RequestMapping(path = UPDATE, method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody ApplicationAnnotation annotation) {
		AnnotationValidityReport report = null;

		try {
			report = storageManager.saveOrUpdate(tag, annotation);
		} catch (IOException e) {
			LOGGER.error("Error during updating annotation with tag {}!", tag);
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
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the system model, it is rejected.
	 *
	 * @param tag
	 *            Tag of the annotation.
	 * @param version
	 * @param annotation
	 *            The annotation model in YAML format.
	 * @return
	 */
	@RequestMapping(path = UPLOAD, method = RequestMethod.PUT)
	public ResponseEntity<String> updateAnnotationYaml(@PathVariable("tag") String tag, @ApiParam(value = "The annotation model in YAML format.", required = true) @RequestBody String annotation) {
		AnnotationValidityReport report = null;

		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		ApplicationAnnotation idpaAnnotation;
		try {
			idpaAnnotation = serializer.readFromYamlString(annotation);
		} catch (IOException e) {
			LOGGER.error("Exception during reading annotation model with tag {}!", tag);
			LOGGER.error("Exception: ", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			report = storageManager.saveOrUpdate(tag, idpaAnnotation);
		} catch (IOException e) {
			LOGGER.error("Error during updating annotation with tag {}!", tag);
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
	 * @param tag
	 *            The tag.
	 * @param version
	 *            The timestamp of the application.
	 * @return A list with the timestamps of all broken annotations.
	 */
	@RequestMapping(path = GET_BROKEN, method = RequestMethod.GET)
	public ResponseEntity<List<String>> getBroken(@PathVariable("tag") String tag, @RequestParam String version) {
		List<String> broken;
		try {
			broken = storageManager.getBrokenForApplication(tag, VersionOrTimestamp.fromString(version)).stream().map(VersionOrTimestamp::toString).collect(Collectors.toList());
		} catch (ParseException e) {
			LOGGER.error("Could not parse timestamp!", e);

			return ResponseEntity.badRequest().body(Arrays.asList("Illegally formatted timestamp: " + version));
		}

		return ResponseEntity.ok(broken);
	}

}
