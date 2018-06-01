package org.continuity.wessbas.controllers;

import static org.continuity.api.rest.RestApi.Wessbas.JMeter.ROOT;
import static org.continuity.api.rest.RestApi.Wessbas.JMeter.Paths.CREATE;

import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.wessbas.entities.WorkloadModelStorageEntry;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.continuity.wessbas.transform.jmeter.WessbasToJmeterConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class JMeterController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterController.class);

	@Autowired
	private WessbasToJmeterConverter jmeterConverter;

	@RequestMapping(value = CREATE, method = RequestMethod.GET)
	public JMeterTestPlanBundle createTestPlan(@PathVariable("id") String workloadModelId) {
		if (workloadModelId == null) {
			throw new IllegalArgumentException("The workload model id is null!");
		}

		WorkloadModelStorageEntry workloadModelEntry = SimpleModelStorage.instance().get(workloadModelId);

		if (workloadModelEntry == null) {
			throw new IllegalArgumentException("There is no workload model with id " + workloadModelId + "!");
		}

		WorkloadModel workloadModel = workloadModelEntry.getWorkloadModel();

		JMeterTestPlanBundle testPlanPack = jmeterConverter.convertToLoadTest(workloadModel);

		LOGGER.info("Created JMeter test plan with id {}.", workloadModelId);

		return testPlanPack;
	}

}
