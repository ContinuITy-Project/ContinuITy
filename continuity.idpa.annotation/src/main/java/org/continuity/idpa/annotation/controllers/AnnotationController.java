package org.continuity.idpa.annotation.controllers;

import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.ROOT;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.Paths.GET;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.Paths.GET_BASE;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.Paths.LEGACY_UPDATE;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.Paths.UPDATE;
import static org.continuity.api.rest.RestApi.IdpaAnnotation.Annotation.Paths.UPLOAD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.storage.AnnotationStorageManager;
import org.continuity.idpa.yaml.IdpaYamlSerializer;
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
	 * Retrieves the specified annotation if present and if it is not broken.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @returnA {@link ResponseEntity} holding the annotation or specifying the error if one
	 *          occurred. If there is no annotation for the tag, the status 404 (Not Found) will be
	 *          returned. If the annotation is broken, a 423 (Locked) will be returned with a link
	 *          to retrieve the annotation, anyway.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<?> getAnnotation(@PathVariable("tag") String tag) {
		ApplicationAnnotation annotation;

		try {
			annotation = storageManager.getAnnotation(tag);
		} catch (IOException e) {
			LOGGER.error("Error during getting annotation with tag {}!", tag);
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (annotation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (storageManager.isBroken(tag)) {
			Map<String, String> message = new HashMap<>();
			message.put("message", "The requested annotation is broken. Get the base via the redirect.");
			message.put("redirect", applicationName + RestApi.IdpaAnnotation.Annotation.GET_BASE.path(tag));
			return new ResponseEntity<>(message, HttpStatus.LOCKED);
		}

		return new ResponseEntity<>(annotation, HttpStatus.OK);
	}

	/**
	 * Retrieves the base of the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @returnA {@link ResponseEntity} holding the annotation or specifying the error if one
	 *          occurred. If there is no base annotation for the tag, the status 404 (Not Found)
	 *          will be returned.
	 */
	@RequestMapping(path = GET_BASE, method = RequestMethod.GET)
	public ResponseEntity<ApplicationAnnotation> getBaseAnnotation(@PathVariable("tag") String tag) {
		ApplicationAnnotation annotation;

		try {
			annotation = storageManager.getBaseAnnotation(tag);
		} catch (IOException e) {
			LOGGER.error("Error during getting base annotation with tag {}!", tag);
			LOGGER.error("Exception:", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (annotation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(annotation, HttpStatus.OK);
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the system model, it is rejected.
	 *
	 * @param tag
	 * @param annotation
	 * @return
	 */
	@RequestMapping(path = UPDATE, method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody ApplicationAnnotation annotation) {
		AnnotationValidityReport report = null;

		try {
			report = storageManager.updateAnnotation(tag, annotation);
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
	 * 				Tag of the annotation.
	 * @param annotation
	 * 				The annotation model in YAML format.
	 * @return
	 */
	@RequestMapping(path = UPLOAD, method = RequestMethod.PUT)
	public ResponseEntity<String> uploadAnnotation(@PathVariable("tag") String tag, 
			@ApiParam(value = "The annotation model in YAML format.", required = true) @RequestBody String annotation) {
		AnnotationValidityReport report = null;

		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
		ApplicationAnnotation idpaAnnotation;
		try {
			idpaAnnotation = serializer.readFromYamlString(annotation);
		} catch (IOException e) {
			LOGGER.error("Exception during reading annotation model with tag {}!", tag);
			LOGGER.error("Exception: " , e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		try {
			report = storageManager.updateAnnotation(tag, idpaAnnotation);
		} catch (IOException e) {
			LOGGER.error("Error during updating annotation with tag {}!", tag);
			LOGGER.error("Exception: " , e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (report.isBreaking()) {
			return new ResponseEntity<>(report.toString(), HttpStatus.CONFLICT);
		} else {
			return new ResponseEntity<>(report.toString(), HttpStatus.CREATED);
		}
	}
	
	/**
	 * Updates a legacy IDPA.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @return A response entity providing a message whether the update was successful.
	 */
	@RequestMapping(path = LEGACY_UPDATE, method = RequestMethod.GET)
	public ResponseEntity<String> updateLegacyIdpa(@PathVariable("tag") String tag) {
		boolean updated;

		try {
			updated = storageManager.updateLegacyIdpa(tag);
		} catch (IOException e) {
			LOGGER.error("Exception when updating the legacy IDPA with tag {}!", tag);
			LOGGER.error("Exception:", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		String message;

		if (updated) {
			message = "The IDPA has been updated to the current version.";
		} else {
			message = "There was no need to update the IDPA.";
		}

		return ResponseEntity.ok(message);
	}

}
