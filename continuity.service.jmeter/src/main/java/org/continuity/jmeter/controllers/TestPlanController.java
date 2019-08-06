package org.continuity.jmeter.controllers;

import static org.continuity.api.rest.RestApi.JMeter.TestPlan.ROOT;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.GET;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.POST;

import java.util.Collections;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.order.LoadTestType;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
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

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

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
	 * @param aid
	 *            the corresponding app-id
	 * @param version
	 *            an optional version. If {@code null}, the latest will be used.
	 * @param annotate
	 *            Indicates whether the test plan should be annotated with the IDPA stored for the
	 *            specified app-id.
	 * @return a {@link ArtifactExchangeModel} containing the link
	 */
	@RequestMapping(value = POST, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<ArtifactExchangeModel> uploadTestPlan(@RequestBody JMeterTestPlanBundle bundle, @ApiIgnore @PathVariable("app-id") AppId aid,
			@RequestParam(required = false) VersionOrTimestamp version, @RequestParam(defaultValue = "false") boolean annotate) {
		LOGGER.info("Received uploaded test plan with app-id {}.", aid);

		if (annotate) {
			LOGGER.info("Annotating test plan with app-id {}.", aid);

			JMeterAnnotator annotator = new JMeterAnnotator(bundle.getTestPlan(), restTemplate);
			ListedHashTree annotatedTestPlan = annotator.createAnnotatedTestPlan(aid, Collections.singletonList(new ServiceSpecification(aid.getService(), version)));
			bundle.setTestPlan(annotatedTestPlan);
		}

		String id = storage.put(bundle, aid);
		return ResponseEntity.ok(new ArtifactExchangeModel().getLoadTestLinks().setType(LoadTestType.JMETER).setLink(RestApi.JMeter.TestPlan.GET.requestUrl(id).withoutProtocol().get()).parent());
	}

}
