package org.continuity.benchflow.wessbas;

import org.continuity.workload.driver.AnnotationNotSupportedException;
import org.continuity.workload.driver.WorkloadConverter;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtension;
import org.continuity.workload.dsl.system.TargetSystem;

import cloud.benchflow.dsl.definition.BenchFlowTest;
import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
public class WessbasToBenchFlowConverter implements WorkloadConverter<WorkloadModel, BenchFlowTest> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BenchFlowTest convertToWorkload(WorkloadModel workloadModel, TargetSystem system, SystemAnnotation annotation, AnnotationExtension extension) throws AnnotationNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BenchFlowTest convertToWorkload(WorkloadModel workloadModel, TargetSystem system, SystemAnnotation annotation) {

		return null;
	}

}
