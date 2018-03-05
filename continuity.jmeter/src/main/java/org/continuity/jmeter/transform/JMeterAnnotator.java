package org.continuity.jmeter.transform;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;

/**
 * @author Henning Schulz
 *
 */
public class JMeterAnnotator {

	private final ListedHashTree testPlan;

	private final SystemModel system;

	public JMeterAnnotator(ListedHashTree testPlan, SystemModel system) {
		this.testPlan = testPlan;
		this.system = system;
	}

	public void addAnnotations(SystemAnnotation annotation) {
		new UserDefinedVarsAnnotator(annotation).annotateVariables(testPlan);
		new HttpSamplersAnnotator(system, annotation).annotateSamplers(testPlan);
		new CounterAnnotator(annotation).addCounters(testPlan);
	}

}
