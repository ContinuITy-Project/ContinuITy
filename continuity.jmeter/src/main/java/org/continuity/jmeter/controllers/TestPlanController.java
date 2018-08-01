package org.continuity.jmeter.controllers;

import static org.continuity.api.rest.RestApi.JMeter.TestPlan.ROOT;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.GET;

import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.commons.storage.MemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for test plans.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class TestPlanController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanController.class);

	@Autowired
	@Qualifier("testPlanStorage")
	private MemoryStorage<JMeterTestPlanBundle> storage;

	/**
	 * Returns the test plan that is stored with the specified ID.
	 *
	 * @param id
	 *            The ID of the test plan.
	 * @return A bundle holding the test plan or a 404 error response if not found.
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<JMeterTestPlanBundle> getTestPlan(@PathVariable String id) {
		JMeterTestPlanBundle bundle = storage.get(id);

		if (bundle == null) {
			LOGGER.warn("Could not find a test plan with id {}!", id);
			return ResponseEntity.notFound().build();
		} else {
			LOGGER.info("Retrieved test plan with id {}.", id);
			return ResponseEntity.ok(bundle);
		}
	}

}
