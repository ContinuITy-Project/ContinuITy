package org.continuity.wessbas.controllers;

import static org.continuity.api.rest.RestApi.Wessbas.BehaviorModel.ROOT;
import static org.continuity.api.rest.RestApi.Wessbas.BehaviorModel.Paths.CREATE;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.transform.benchflow.WessbasToBehaviorModelConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import m4jdsl.WorkloadModel;

/**
 * Controls the creation of {@link BehaviorModel} from a stored WESSBAS model.
 * 
 * @author Manuel Palenga
 *
 */
@RestController
@RequestMapping(ROOT)
public class BehaviorModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterController.class);

	@Autowired
	private MixedStorage<WessbasBundle> storage;
	
	@Autowired
	private WessbasToBehaviorModelConverter behaviorModelConverter;
	
	/**
	 * Gets an behavior model of the model with the passed id.
	 * 
	 * @param workloadModelId
	 * 			The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(value = CREATE, method = RequestMethod.GET)
	public ResponseEntity<BehaviorModel> getBehaviorModel(@PathVariable("id") String workloadModelId) {
		if (workloadModelId == null) {
			throw new IllegalArgumentException("The workload model id is null!");
		}

		WessbasBundle wessbasBundleEntry = storage.get(workloadModelId);
		if (wessbasBundleEntry == null) {
			return ResponseEntity.notFound().build();
		}

		WorkloadModel workloadModel = wessbasBundleEntry.getWorkloadModel();

		BehaviorModel behaviorModel = behaviorModelConverter.convertToBehaviorModel(workloadModel);

		LOGGER.info("Created behavior model with id {}.", workloadModelId);

		return ResponseEntity.ok(behaviorModel);
	}

}
