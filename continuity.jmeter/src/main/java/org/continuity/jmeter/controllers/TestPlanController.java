package org.continuity.jmeter.controllers;

import static org.continuity.api.rest.RestApi.JMeter.TestPlan.ROOT;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.GET;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.POST;

import java.util.Collections;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
	private MixedStorage<JMeterTestPlanBundle> storage;

	@Autowired
	private RestTemplate restTemplate;

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

	/**
	 * Uploads a new Jmeter Testplan
	 *
	 * @param bundle
	 *            {@link JMeterTestPlanBundle}
	 * @param tag
	 *            the corresponding tag
	 * @param annotate
	 *            Indicates whether the test plan should be annotated with the IDPA stored for the
	 *            specified tag.
	 * @return a {@link LinkExchangeModel} containing the link
	 */
	@RequestMapping(value = POST, method = RequestMethod.POST)
	public ResponseEntity<LinkExchangeModel> uploadTestPlan(@RequestBody JMeterTestPlanBundle bundle, @PathVariable String tag, @RequestParam(defaultValue = "false") boolean annotate) {
		LOGGER.info("Received uploaded test plan with tag {}.", tag);

		if (annotate) {
			LOGGER.info("Annotating test plan with tag {}.", tag);

			JMeterAnnotator annotator = new JMeterAnnotator(bundle.getTestPlan(), restTemplate);
			ListedHashTree annotatedTestPlan = annotator.createAnnotatedTestPlan(Collections.singletonList(tag));
			bundle.setTestPlan(annotatedTestPlan);
		}

		String id = storage.put(bundle, tag);
		return ResponseEntity.ok(new LinkExchangeModel().getLoadTestLinks().setType(LoadTestType.JMETER).setLink(RestApi.JMeter.TestPlan.GET.requestUrl(id).withoutProtocol().get()).parent());
	}

}
