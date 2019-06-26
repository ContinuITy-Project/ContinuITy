package org.continuity.request.rates.controllers;

import static org.continuity.api.rest.RestApi.RequestRates.JMeter.ROOT;
import static org.continuity.api.rest.RestApi.RequestRates.JMeter.Paths.CREATE;

import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.request.rates.model.RequestRatesModel;
import org.continuity.request.rates.transform.RequestRatesToJMeterConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class JMeterController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterController.class);

	@Autowired
	private MixedStorage<RequestRatesModel> storage;

	@RequestMapping(value = CREATE, method = RequestMethod.GET)
	public JMeterTestPlanBundle createTestPlan(@PathVariable("id") String workloadModelId) {
		if (workloadModelId == null) {
			throw new IllegalArgumentException("The request rates model id is null!");
		}

		RequestRatesModel model = storage.get(workloadModelId);

		if (model == null) {
			throw new IllegalArgumentException("There is no request rates model with id " + workloadModelId + "!");
		}

		RequestRatesToJMeterConverter jmeterConverter = new RequestRatesToJMeterConverter();
		JMeterTestPlanBundle testPlanPack = jmeterConverter.convertToLoadTest(model);

		LOGGER.info("Created JMeter test plan with id {}.", workloadModelId);

		return testPlanPack;
	}

}
