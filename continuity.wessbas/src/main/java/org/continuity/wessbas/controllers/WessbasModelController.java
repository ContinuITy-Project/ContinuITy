package org.continuity.wessbas.controllers;

import static org.continuity.api.rest.RestApi.Wessbas.Model.ROOT;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.GET_ANNOTATION;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.GET_APPLICATION;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.GET_WORKLOAD;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.OVERVIEW;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.PERSIST;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.REMOVE;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.RESERVE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.continuity.wessbas.entities.WorkloadModelStorageEntry;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import wessbas.commons.util.XmiEcoreHandler;

/**
 * Controls the created WESSBAS models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class WessbasModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasModelController.class);

	private final ConcurrentMap<String, Application> systemModelBuffer = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, ApplicationAnnotation> annotationBuffer = new ConcurrentHashMap<>();

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${persist.path:persisted}")
	private String persistPath;

	/**
	 * Gets an overview of the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(path = OVERVIEW, method = RequestMethod.GET)
	public ResponseEntity<WorkloadModelPack> getOverview(@PathVariable String id) {
		WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);
		if (entry == null) {
			return ResponseEntity.notFound().build();
		}

		String tag = id.substring(0, id.lastIndexOf("-"));

		return ResponseEntity.ok(new WorkloadModelPack(applicationName, id, tag));
	}

	/**
	 * Gets properties of the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(path = GET_WORKLOAD, method = RequestMethod.GET)
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
	@RequestMapping(path = REMOVE, method = RequestMethod.DELETE)
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
	 * @return An application model for the stored WESSBAS model or a 404 (Not Found) if there is no
	 *         such model.
	 */
	@RequestMapping(path = GET_APPLICATION, method = RequestMethod.GET)
	public ResponseEntity<Application> getApplicationModel(@PathVariable String id) {
		Application systemModel = createIdpa(id).getLeft();

		if (systemModel == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(systemModel);
		}
	}

	/**
	 * Gets an annotation model for the WESSBAS model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return An annotation for the stored WESSBAS model or a 404 (Not Found) if there is no such
	 *         model.
	 */
	@RequestMapping(path = GET_ANNOTATION, method = RequestMethod.GET)
	public ResponseEntity<ApplicationAnnotation> getApplicationAnnotation(@PathVariable String id) {
		ApplicationAnnotation annotation = createIdpa(id).getRight();

		if (annotation == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(annotation);
		}
	}

	private Pair<Application, ApplicationAnnotation> createIdpa(String id) {
		Application systemModel = systemModelBuffer.get(id);
		ApplicationAnnotation annotation = annotationBuffer.get(id);

		if ((systemModel == null) || (annotation == null)) {
			WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);

			if (entry == null) {
				return Pair.of(null, null);
			}

			AnnotationFromWessbasExtractor annotationExtractor = new AnnotationFromWessbasExtractor();
			annotationExtractor.init(entry.getWorkloadModel(), id);

			systemModel = annotationExtractor.extractSystemModel();
			if (entry.getDataTimestamp() != null) {
				systemModel.setTimestamp(entry.getDataTimestamp());
			}

			annotation = annotationExtractor.extractInitialAnnotation();

			annotationBuffer.put(id, annotation);
		}

		return Pair.of(systemModel, annotation);
	}

	/**
	 * Reserves a workload model entry in the storage.
	 *
	 * @param tag
	 *            The tag of the workload model.
	 * @return A response entity holding a link to the workload model that will be created. The link
	 *         is invalid until the creation is finished.
	 */
	@RequestMapping(path = RESERVE, method = RequestMethod.GET)
	public ResponseEntity<String> reserveModelLink(@PathVariable String tag) {
		String storageId = SimpleModelStorage.instance().reserve(tag);
		String link = applicationName + RestApi.Wessbas.Model.OVERVIEW.path(storageId);

		LOGGER.info("Reserved workload model entry {}", storageId);

		return ResponseEntity.created(URI.create(link)).body(link);
	}

	/**
	 * Persists the model with the passed id to the file system.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The path to the persisted model or a 404 (Not Found) if there is no such model.
	 * @throws IOException
	 *             If an error during persisting occurs.
	 */
	@RequestMapping(path = PERSIST, method = RequestMethod.GET)
	public ResponseEntity<String> persist(@PathVariable String id) throws IOException {
		WorkloadModelStorageEntry entry = SimpleModelStorage.instance().get(id);

		if (entry == null) {
			LOGGER.warn("Could not persist model {}. This model does not exist!", id);
			return ResponseEntity.notFound().build();
		} else {
			String file = "wessbas-model-" + id + ".xmi";
			Path dir = Paths.get(persistPath);
			dir.toFile().mkdirs();
			Path path = dir.resolve(file);

			XmiEcoreHandler.getInstance().ecoreToXMI(entry.getWorkloadModel(), path.toString());

			LOGGER.warn("Persisted model {} to {}", id, path);
			return ResponseEntity.ok(path.toAbsolutePath().toString());
		}
	}

}
