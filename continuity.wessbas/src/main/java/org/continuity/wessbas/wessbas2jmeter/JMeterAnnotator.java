package org.continuity.wessbas.wessbas2jmeter;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.TargetSystem;

/**
 * @author Henning Schulz
 *
 */
public class JMeterAnnotator {

	private final ListedHashTree testPlan;

	private final TargetSystem system;

	public JMeterAnnotator(ListedHashTree testPlan, TargetSystem system) {
		this.testPlan = testPlan;
		this.system = system;
	}

	public void addAnnotations(SystemAnnotation annotation) {
		// TODO: Input data
		new HttpSamplersAnnotator(system, annotation).annotateSamplers(testPlan);
	}

}
