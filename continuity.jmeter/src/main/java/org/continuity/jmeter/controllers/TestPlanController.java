package org.continuity.jmeter.controllers;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.utils.WebUtils;
import org.continuity.jmeter.amqp.TestPlanAmqpHandler;
import org.continuity.jmeter.entities.TestPlanBundle;
import org.continuity.jmeter.entities.WorkloadLinks;
import org.continuity.jmeter.transform.JMeterAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Controls creation and execution of JMeter test plans.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("loadtest")
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
	@RequestMapping(value = "{type}/model/{id}/create", method = RequestMethod.GET)
	public TestPlanBundle createAndGetLoadTest(@PathVariable("type") String workloadModelType, @PathVariable("id") String workloadModelId, @RequestParam String tag) {
		return createAndGetLoadTest(workloadModelType + "/model/" + workloadModelId, tag);
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
	public TestPlanBundle createAndGetLoadTest(String workloadModelLink, String tag) {
		LOGGER.debug("Creating a load test from {}.", workloadModelLink);

		WorkloadLinks workloadLinks = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadModelLink), WorkloadLinks.class);

		if ((workloadLinks == null) || (workloadLinks.getJmeterLink() == null)) {
			throw new IllegalArgumentException("The workload model at " + workloadModelLink + " cannot be transformed into JMeter!");
		}

		TestPlanBundle testPlanPack = restTemplate.getForObject(WebUtils.addProtocolIfMissing(workloadLinks.getJmeterLink()), TestPlanBundle.class);

		ListedHashTree annotatedTestPlan = createAnnotatedTestPlan(testPlanPack, tag);

		if (annotatedTestPlan == null) {
			LOGGER.error("Could not annotate the test plan! Ignoring the annotation.");
			annotatedTestPlan = testPlanPack.getTestPlan();
		}

		return new TestPlanBundle(annotatedTestPlan, testPlanPack.getBehaviors());
	}

	private ListedHashTree createAnnotatedTestPlan(TestPlanBundle testPlanPack, String tag) {
		SystemAnnotation annotation;
		try {
			annotation = restTemplate.getForObject(getAnnotationLink(tag, "annotation"), SystemAnnotation.class);
		} catch (RestClientException e) {
			e.printStackTrace();
			return null;
		}

		if (annotation == null) {
			LOGGER.error("Annotation with tag {} is null! Aborting.", tag);
			return null;
		}

		SystemModel systemModel = restTemplate.getForObject(getAnnotationLink(tag, "system"), SystemModel.class);

		if (systemModel == null) {
			LOGGER.error("System with tag {} is null! Aborting.", tag);
			return null;
		}

		ListedHashTree testPlan = testPlanPack.getTestPlan();
		JMeterAnnotator annotator = new JMeterAnnotator(testPlan, systemModel);
		annotator.addAnnotations(annotation);

		return testPlan;
	}

	private String getAnnotationLink(String tag, String suffix) {
		return "http://workload-annotation/ann/" + tag + "/" + suffix;
	}

}
