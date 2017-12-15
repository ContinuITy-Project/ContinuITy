package org.continuity.workload.annotation.controllers;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.workload.annotation.entities.AnnotationValidityReport;
import org.continuity.workload.annotation.storage.AnnotationStorage;
import org.continuity.workload.annotation.validation.AnnotationValidityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("/ann")
public class AnnotationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

	private final AnnotationStorage storage;

	@Autowired
	public AnnotationController(AnnotationStorage storage) {
		this.storage = storage;
	}

	/**
	 * Retrieves the specified system model if present.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return A {@link ResponseEntity} holding the system model or specifying the error if one
	 *         occurred. If there is no system model for the tag, the status 404 (Not Found) will be
	 *         returned.
	 */
	@RequestMapping(path = "{tag}/system", method = RequestMethod.GET)
	public ResponseEntity<SystemModel> getSystemModel(@PathVariable("tag") String tag) {
		SystemModel systemModel;

		try {
			systemModel = storage.readSystemModel(tag);
		} catch (IOException e) {
			LOGGER.error("Could not read system model with tag {}!", tag);
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (systemModel == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(systemModel, HttpStatus.OK);
	}

	/**
	 * Retrieves the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @returnA {@link ResponseEntity} holding the annotation or specifying the error if one
	 *          occurred. If there is no annotation for the tag, the status 404 (Not Found) will be
	 *          returned.
	 */
	@RequestMapping(path = "{tag}/annotation", method = RequestMethod.GET)
	public ResponseEntity<SystemAnnotation> getAnnotation(@PathVariable("tag") String tag) {
		SystemAnnotation annotation;

		try {
			annotation = storage.readAnnotation(tag);
		} catch (IOException e) {
			LOGGER.error("Could not read annotation with tag {}!", tag);
			e.printStackTrace();
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
	@RequestMapping(path = "{tag}/annotation", method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody SystemAnnotation annotation) {
		SystemModel systemModel = null;
		AnnotationValidityReport report = null;

		try {
			systemModel = storage.readSystemModel(tag);
		} catch (IOException e) {
			LOGGER.error("Could not read system model with tag {}!", tag);
			e.printStackTrace();
		}

		if (systemModel != null) {
			AnnotationValidityChecker checker = new AnnotationValidityChecker(systemModel);
			checker.checkAnnotation(annotation);
			report = checker.getReport();

			if (report.isBreaking()) {
				return new ResponseEntity<>(report.toString(), HttpStatus.CONFLICT);
			}
		}

		boolean overwritten;

		try {
			overwritten = storage.saveOrUpdate(tag, annotation);
		} catch (IOException e) {
			LOGGER.error("Could not save annotation with tag {}!", tag);
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		String message = "Annotation has been " + (overwritten ? "updated" : "created");

		if ((report != null) && !report.isOk()) {
			message += " with warnings: " + report;
		}

		return new ResponseEntity<>(message, HttpStatus.CREATED);
	}

}
