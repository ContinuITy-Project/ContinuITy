package org.continuity.jmeter.controllers;

import static org.continuity.api.rest.RestApi.JMeter.TestPlan.ROOT;
import static org.continuity.api.rest.RestApi.JMeter.TestPlan.Paths.CREATE_AND_GET;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.IdpaAnnotation;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.jmeter.amqp.TestPlanAmqpHandler;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Controls creation and execution of JMeter test plans.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class TestPlanController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestPlanAmqpHandler.class);

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Transforms a workload model into a JMeter test and returns it. The workload model is
	 * specified by a decomposed link: {@code workloadModelType/model/workloadModelId}.
	 *
	 * @param workloadModelType
	 *            The type of the workload model, e.g., wessbas.
	 * @param workloadModelId
	 *            The id of the workload model.
	 * @param tag
	 *            The tag to be used to retrieve the annotation.
	 * @return The transformed JMeter test plan.
	 */
	@RequestMapping(value = CREATE_AND_GET, method = RequestMethod.GET)
	public JMeterTestPlanBundle createAndGetLoadTest(@PathVariable("type") String workloadModelType, @PathVariable("id") String workloadModelId, @RequestParam String tag) {
		return createAndGetLoadTest(RestApi.Generic.WORKLOAD_MODEL_LINK.get(workloadModelType).requestUrl(workloadModelId).get(), tag);
	}

	/**
	 * Transforms a workload model into a JMeter test and returns it. The workload model is
	 * specified by a link, e.g., {@code TYPE/model/ID}.
	 *
	 * @param workloadModelLink
	 *            The link pointing to the workload model. When called, it is supposed to return an
	 *            object containing a field {@code jmeter-link} which holds a link to the
	 *            corresponding JMeter test plan.
	 * @param tag
	 *            The tag to be used to retrieve the annotation.
	 * @return The transformed JMeter test plan.
	 */
	public JMeterTestPlanBundle createAndGetLoadTest(String workloadModelLink, String tag) {
		LOGGER.debug("Creating a load test from {}.", workloadModelLink);

		LinkExchangeModel workloadLinks = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadModelLink), LinkExchangeModel.class);

		if ((workloadLinks == null) || (workloadLinks.getJmeterLink() == null)) {
			throw new IllegalArgumentException("The workload model at " + workloadModelLink + " cannot be transformed into JMeter!");
		}

		JMeterTestPlanBundle testPlanPack = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadLinks.getJmeterLink()), JMeterTestPlanBundle.class);

		ListedHashTree annotatedTestPlan = createAnnotatedTestPlan(testPlanPack, tag);

		if (annotatedTestPlan == null) {
			LOGGER.error("Could not annotate the test plan! Ignoring the annotation.");
			annotatedTestPlan = testPlanPack.getTestPlan();
		}

		return new JMeterTestPlanBundle(annotatedTestPlan, testPlanPack.getBehaviors());
	}

	private ListedHashTree createAnnotatedTestPlan(JMeterTestPlanBundle testPlanPack, String tag) {
		ApplicationAnnotation annotation;
		try {
			annotation = restTemplate.getForObject(IdpaAnnotation.Annotation.GET.requestUrl(tag).get(), ApplicationAnnotation.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Received a non-200 response: {} ({}) - {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase(), e.getResponseBodyAsString());
			return null;
		}

		if (annotation == null) {
			LOGGER.error("Annotation with tag {} is null! Aborting.", tag);
			return null;
		}

		Application application = restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);

		if (application == null) {
			LOGGER.error("Application with tag {} is null! Aborting.", tag);
			return null;
		}

		ListedHashTree testPlan = testPlanPack.getTestPlan();
		JMeterAnnotator annotator = new JMeterAnnotator(testPlan, application);
		annotator.addAnnotations(annotation);

		return testPlan;
	}

}
