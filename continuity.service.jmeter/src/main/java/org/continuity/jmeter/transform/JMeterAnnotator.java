package org.continuity.jmeter.transform;

import java.util.List;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.Idpa;
import org.continuity.idpa.AppId;
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
	 * @param appIds
	 * @return
	 */
	public ListedHashTree createAnnotatedTestPlan(AppId aid, List<ServiceSpecification> services) {
		new UserDefinedDefaultVariablesCleanerAnnotator().cleanVariables(testPlan);

		for (ServiceSpecification service : services) {
			ApplicationAnnotation annotation;
			try {
				annotation = restTemplate.getForObject(Idpa.Annotation.GET.requestUrl(aid.withService(service.getService())).withQueryIfNotEmpty("version", service.getVersion().toString()).get(),
						ApplicationAnnotation.class);
			} catch (HttpStatusCodeException e) {
				LOGGER.error("Received a non-200 response: {} ({}) - {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase(), e.getResponseBodyAsString());
				continue;
			}
			if (annotation == null) {
				LOGGER.error("Annotation with app-id {} is null! Aborting.", aid);
				continue;
			}
			Application application = restTemplate.getForObject(
					RestApi.Idpa.Application.GET.requestUrl(aid.withService(service.getService())).withQueryIfNotEmpty("version", service.getVersion().toString()).get(), Application.class);
			if (application == null) {
				LOGGER.error("Application with app-id {} is null! Aborting.", aid);
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
