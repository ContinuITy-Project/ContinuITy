package org.continuity.wessbas.controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.continuity.wessbas.entities.WorkloadModelStorageEntry;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controls the created WESSBAS models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("/model")
public class WessbasModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasModelController.class);

	private final ConcurrentMap<String, SystemModel> systemModelBuffer = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, SystemAnnotation> annotationBuffer = new ConcurrentHashMap<>();

	/**
	 * Gets an overview of the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(path = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<WorkloadModelPack> getOverview(@PathVariable String id) {
		WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);
		if (entry == null) {
			return ResponseEntity.notFound().build();
		}

		String tag = id.substring(0, id.lastIndexOf("-"));

		return ResponseEntity.ok(new WorkloadModelPack(getHostname(), id, tag));
	}

	/**
	 * Gets properties of the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(path = "/{id}/workload", method = RequestMethod.GET)
	public ResponseEntity<WorkloadModelStorageEntry> getModel(@PathVariable String id) {
		WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);

		if (entry == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(entry);
		}
	}

	/**
	 * Deletes the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return 200 (Ok) if the model has been successfully deleted or 404 (Not Found) otherwise.
	 */
	@RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> removeModel(@PathVariable String id) {
		boolean succ = SimpleModelStorage.instance().remove(id);

		if (succ) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Gets an annotation model for the WESSBAS model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return A system model for the stored WESSBAS model or a 404 (Not Found) if there is no such
	 *         model.
	 */
	@RequestMapping(path = "{id}/system", method = RequestMethod.GET)
	public ResponseEntity<SystemModel> getSystemModel(@PathVariable String id) {
		SystemModel systemModel = systemModelBuffer.get(id);

		if (systemModel == null) {
			WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);

			if (entry == null) {
				return ResponseEntity.notFound().build();
			}

			AnnotationFromWessbasExtractor annotationExtractor = new AnnotationFromWessbasExtractor();
			annotationExtractor.init(entry.getWorkloadModel(), id);

			systemModel = annotationExtractor.extractSystemModel();
			SystemAnnotation annotation = annotationExtractor.extractInitialAnnotation();

			annotationBuffer.put(id, annotation);
		}

		return ResponseEntity.ok(systemModel);
	}

	/**
	 * Gets an annotation model for the WESSBAS model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return An annotation for the stored WESSBAS model or a 404 (Not Found) if there is no such
	 *         model.
	 */
	@RequestMapping(path = "{id}/annotation", method = RequestMethod.GET)
	public ResponseEntity<SystemAnnotation> getSystemAnnotation(@PathVariable String id) {
		SystemAnnotation annotation = annotationBuffer.get(id);

		if (annotation == null) {
			WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);

			if (entry == null) {
				return ResponseEntity.notFound().build();
			}

			AnnotationFromWessbasExtractor annotationExtractor = new AnnotationFromWessbasExtractor();
			annotationExtractor.init(entry.getWorkloadModel(), id);

			annotation = annotationExtractor.extractInitialAnnotation();
			SystemModel systemModel = annotationExtractor.extractSystemModel();

			systemModelBuffer.put(id, systemModel);
		}

		return ResponseEntity.ok(annotation);
	}

	private String getHostname() {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.error("Could not get hostname! Returning 'UNKNOWN'.");
			e.printStackTrace();
			hostname = "UNKNOWN";
		}

		return hostname;
	}

}
