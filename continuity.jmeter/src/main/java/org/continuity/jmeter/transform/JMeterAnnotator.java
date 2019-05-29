package org.continuity.jmeter.transform;

import java.util.List;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.rest.RestApi.IdpaAnnotation;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
public class JMeterAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMeterAnnotator.class);

	private final ListedHashTree testPlan;

	private final RestTemplate restTemplate;

	public JMeterAnnotator(ListedHashTree testPlan, RestTemplate restTemplate) {
		this.testPlan = testPlan;
		this.restTemplate = restTemplate;
	}

	/**
	 *
	 *
	 * @param testPlanPack
	 * @param tags
	 * @return
	 */
	public ListedHashTree createAnnotatedTestPlan(List<String> tags) {
		new UserDefinedDefaultVariablesCleanerAnnotator().cleanVariables(testPlan);

		for (String tag : tags) {
			ApplicationAnnotation annotation;
			try {
				annotation = restTemplate.getForObject(IdpaAnnotation.Annotation.GET.requestUrl(tag).get(), ApplicationAnnotation.class);
			} catch (HttpStatusCodeException e) {
				LOGGER.error("Received a non-200 response: {} ({}) - {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase(), e.getResponseBodyAsString());
				continue;
			}
			if (annotation == null) {
				LOGGER.error("Annotation with tag {} is null! Aborting.", tag);
				continue;
			}
			Application application = restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);
			if (application == null) {
				LOGGER.error("Application with tag {} is null! Aborting.", tag);
				continue;
			}

			addAnnotations(application, annotation);
		}
		return testPlan;
	}

	private void addAnnotations(Application application, ApplicationAnnotation annotation) {
		new UserDefinedVarsAnnotator(annotation).annotateVariables(testPlan);
		new CSVDataSetAnnotator(annotation).addCsvDataSetConfigs(testPlan);
		new HttpSamplersAnnotator(application, annotation).annotateSamplers(testPlan);
		ValueExtractorsAnnotator vxa = new ValueExtractorsAnnotator(application, annotation);
		vxa.annotateSamplers(testPlan);
		vxa.annotateInputs(testPlan);
		new CounterAnnotator(annotation).addCounters(testPlan);
		new HeadersAnnotator(application, annotation).annotateSamplers(testPlan);
	}

}
