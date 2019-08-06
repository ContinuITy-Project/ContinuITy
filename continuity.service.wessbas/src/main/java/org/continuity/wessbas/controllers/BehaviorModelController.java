package org.continuity.wessbas.controllers;

import static org.continuity.api.rest.RestApi.Wessbas.BehaviorModel.ROOT;
import static org.continuity.api.rest.RestApi.Wessbas.BehaviorModel.Paths.GET;
import static org.continuity.api.rest.RestApi.Wessbas.BehaviorModel.Paths.GET_LEGACY;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.wessbas.entities.BehaviorModelPack;
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
import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

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

	private static final String FILENAME = "behaviormodel";

	private static final String FILE_EXT = ".csv";

	@Autowired
	private MixedStorage<BehaviorModelPack> behaviorStorage;

	@Autowired
	private MixedStorage<WessbasBundle> workloadStorage;

	@Autowired
	private WessbasToBehaviorModelConverter behaviorModelConverter;

	private CSVHandler csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

	/**
	 * Gets the behavior model for a given behavior model storage ID.
	 *
	 * @param storageId
	 *            The storage ID.
	 * @return The behavior model as {@link RelativeMarkovChain}.
	 * @throws IOException
	 * @throws NullPointerException
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<?> getBehaviorModelPack(@PathVariable("id") String storageId) throws NullPointerException, IOException {
		BehaviorModelPack pack = behaviorStorage.get(storageId);

		if (pack == null) {
			return ResponseEntity.notFound().build();
		} else {
			try {
				return ResponseEntity.ok(convertBehaviorModel(pack));
			} catch (FileNotFoundException e) {
				return ResponseEntity.badRequest().body("Unknown storage ID: " + storageId);
			}
		}
	}

	private MarkovBehaviorModel convertBehaviorModel(BehaviorModelPack behaviorModelPack) throws FileNotFoundException, NullPointerException, IOException {
		MarkovBehaviorModel behaviorModel = new MarkovBehaviorModel();

		for (SessionsBundle sessionBundle : behaviorModelPack.getSessionsBundlePack().getSessionsBundles()) {
			String behaviorFile = behaviorModelPack.getPathToBehaviorModelFiles().resolve("behaviormodelextractor").resolve(FILENAME + sessionBundle.getBehaviorId() + FILE_EXT).toFile().toString();
			RelativeMarkovChain markovChain = RelativeMarkovChain.fromCsv(csvHandler.readValues(behaviorFile));
			markovChain.setId(FILENAME + sessionBundle.getBehaviorId());

			behaviorModel.addMarkovChain(markovChain);
		}

		return behaviorModel;
	}

	/**
	 * Gets an behavior model of the model with the passed id.
	 *
	 * @param workloadModelId
	 * 			The id of the stored model.
	 * @return The stored model or a 404 (Not Found) if there is no such model.
	 */
	@RequestMapping(value = GET_LEGACY, method = RequestMethod.GET)
	public ResponseEntity<BehaviorModel> getBehaviorModel(@PathVariable("id") String workloadModelId) {
		if (workloadModelId == null) {
			throw new IllegalArgumentException("The workload model id is null!");
		}

		WessbasBundle wessbasBundleEntry = workloadStorage.get(workloadModelId);
		if (wessbasBundleEntry == null) {
			return ResponseEntity.notFound().build();
		}

		WorkloadModel workloadModel = wessbasBundleEntry.getWorkloadModel();

		BehaviorModel behaviorModel = behaviorModelConverter.convertToBehaviorModel(workloadModel);

		LOGGER.info("Created behavior model with id {}.", workloadModelId);

		return ResponseEntity.ok(behaviorModel);
	}

}
