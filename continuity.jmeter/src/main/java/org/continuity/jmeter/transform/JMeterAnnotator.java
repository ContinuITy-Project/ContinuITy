package org.continuity.jmeter.transform;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;

/**
 * @author Henning Schulz
 *
 */
public class JMeterAnnotator {

	private final ListedHashTree testPlan;

	private final Application system;

	public JMeterAnnotator(ListedHashTree testPlan, Application system) {
		this.testPlan = testPlan;
		this.system = system;
	}

	public void addAnnotations(ApplicationAnnotation annotation) {
		new UserDefinedVarsAnnotator(annotation).annotateVariables(testPlan);
		new HttpSamplersAnnotator(system, annotation).annotateSamplers(testPlan);
		new ValueExtractorsAnnotator(system, annotation).annotateSamplers(testPlan);
		new CounterAnnotator(annotation).addCounters(testPlan);
		new HeadersAnnotator(system, annotation).annotateSamplers(testPlan);
	}

}
