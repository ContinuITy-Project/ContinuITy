package org.continuity.wessbas.controllers;

import static org.continuity.api.rest.RestApi.Wessbas.Model.ROOT;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.GET_ANNOTATION;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.GET_APPLICATION;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.OVERVIEW;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.UPLOAD;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.PERSIST;
import static org.continuity.api.rest.RestApi.Wessbas.Model.Paths.REMOVE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.wessbas.WessbasModelParser;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.entities.WorkloadModelPack;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

	@Autowired
	private MixedStorage<WessbasBundle> storage;

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
		WessbasBundle workloadModel = storage.get(id);
		if (workloadModel == null) {
			return ResponseEntity.notFound().build();
		}

		String tag = storage.getTagForId(id);

		return ResponseEntity.ok(new WorkloadModelPack(applicationName, id, tag));
	}
	
	/**
	 * Stores the provided workload model with the defined tag.
	 * 
	 * @param tag 
	 * 				The tag of the stored model.
	 * @param wessbasModel
	 * 				The workload model.
	 * @return The new stored model.
	 */
	@RequestMapping(path = UPLOAD, method = RequestMethod.PUT)
	public ResponseEntity<WorkloadModelPack> uploadModel(@PathVariable String tag, @RequestBody String workloadModel) {
		
	    InputStream inputStream = new ByteArrayInputStream(workloadModel.getBytes());
		
		WessbasModelParser parser = new WessbasModelParser();
		
		m4jdsl.WorkloadModel wessbasModel;
		try {
			wessbasModel = parser.readWorkloadModel(inputStream);
		} catch (IOException e) {
			LOGGER.error("Exception while reading workload model with tag " + tag, e);
			throw new IllegalArgumentException("Exception while reading workload model! Content is not allowed.", e);
		}
		
		WessbasBundle bundle = new WessbasBundle(new Date(), wessbasModel);
		String storedItemId = storage.put(bundle, tag);

		return ResponseEntity.ok(new WorkloadModelPack(applicationName, storedItemId, tag));
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
		boolean succ = storage.remove(id);

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
			WessbasBundle bundle = storage.get(id);

			if (bundle == null) {
				return Pair.of(null, null);
			}

			AnnotationFromWessbasExtractor annotationExtractor = new AnnotationFromWessbasExtractor();
			annotationExtractor.init(bundle.getWorkloadModel(), id);

			systemModel = annotationExtractor.extractSystemModel();
			if (bundle.getTimestamp() != null) {
				systemModel.setTimestamp(bundle.getTimestamp());
			}

			annotation = annotationExtractor.extractInitialAnnotation();

			annotationBuffer.put(id, annotation);
		}

		return Pair.of(systemModel, annotation);
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
	@RequestMapping(path = PERSIST, method = RequestMethod.POST)
	@Deprecated
	public ResponseEntity<String> persist(@PathVariable String id) throws IOException {
		WessbasBundle workloadModel = storage.get(id);

		if (workloadModel == null) {
			LOGGER.warn("Could not persist model {}. This model does not exist!", id);
			return ResponseEntity.notFound().build();
		} else {
			String file = "wessbas-model-" + id + ".xmi";
			Path dir = Paths.get(persistPath);
			dir.toFile().mkdirs();
			Path path = dir.resolve(file);

			XmiEcoreHandler.getInstance().ecoreToXMI(workloadModel.getWorkloadModel(), path.toString());

			LOGGER.warn("Persisted model {} to {}", id, path);
			return ResponseEntity.ok(path.toAbsolutePath().toString());
		}
	}

}
