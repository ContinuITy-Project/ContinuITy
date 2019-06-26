package org.continuity.request.rates.controllers;

import static org.continuity.api.rest.RestApi.RequestRates.Model.ROOT;
import static org.continuity.api.rest.RestApi.RequestRates.Model.Paths.GET_ANNOTATION;
import static org.continuity.api.rest.RestApi.RequestRates.Model.Paths.GET_APPLICATION;
import static org.continuity.api.rest.RestApi.RequestRates.Model.Paths.GET_MODEL;
import static org.continuity.api.rest.RestApi.RequestRates.Model.Paths.OVERVIEW;
import static org.continuity.api.rest.RestApi.RequestRates.Model.Paths.REMOVE;

import java.util.List;
import java.util.stream.Collectors;

import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.request.rates.entities.WorkloadModelPack;
import org.continuity.request.rates.model.RequestFrequency;
import org.continuity.request.rates.model.RequestRatesModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controls the created request rates models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class RequestRatesModelController {

	@Autowired
	private MixedStorage<RequestRatesModel> storage;

	@Value("${spring.application.name}")
	private String applicationName;

	private final AnnotationExtractor extractor = new AnnotationExtractor();

	/**
	 * Gets an overview of the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(path = OVERVIEW, method = RequestMethod.GET)
	public ResponseEntity<WorkloadModelPack> getOverview(@PathVariable String id) {
		RequestRatesModel model = storage.get(id);

		if (model == null) {
			return ResponseEntity.notFound().build();
		}

		String tag = storage.getTagForId(id);

		return ResponseEntity.ok(new WorkloadModelPack(applicationName, id, tag));
	}

	/**
	 * Gets the model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(path = GET_MODEL, method = RequestMethod.GET)
	public ResponseEntity<RequestRatesModel> getModel(@PathVariable String id) {
		RequestRatesModel model = storage.get(id);

		if (model == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(model);
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
		boolean succ = storage.remove(id);

		if (succ) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Gets an annotation model for the request rates model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return An application model for the stored request rates model or a 404 (Not Found) if there
	 *         is no such model.
	 */
	@RequestMapping(path = GET_APPLICATION, method = RequestMethod.GET)
	public ResponseEntity<Application> getApplicationModel(@PathVariable String id) {
		RequestRatesModel model = storage.get(id);

		if (model == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(createApplicationModel(model, id));
		}
	}

	/**
	 * Gets an annotation model for the request rates model with the passed id.
	 *
	 * @param id
	 *            The id of the stored model.
	 * @return An annotation for the stored request rates model or a 404 (Not Found) if there is no
	 *         such model.
	 */
	@RequestMapping(path = GET_ANNOTATION, method = RequestMethod.GET)
	public ResponseEntity<ApplicationAnnotation> getInitialAnnotation(@PathVariable String id) {
		RequestRatesModel model = storage.get(id);

		if (model == null) {
			return ResponseEntity.notFound().build();
		} else {
			ApplicationAnnotation ann = extractor.extractAnnotation(createApplicationModel(model, id));

			return ResponseEntity.ok(ann);
		}
	}

	private Application createApplicationModel(RequestRatesModel model, String id) {
		List<Endpoint<?>> endpoints = model.getMix().stream().map(RequestFrequency::getEndpoint).collect(Collectors.toList());
		Application app = new Application();
		app.setId(id);
		app.setEndpoints(endpoints);

		return app;
	}

}
