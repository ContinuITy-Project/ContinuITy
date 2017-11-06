package org.continuity.benchflow.wessbas;

import org.continuity.commons.exceptions.AnnotationNotSupportedException;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtension;
import org.continuity.workload.dsl.system.TargetSystem;

import cloud.benchflow.dsl.definition.BenchFlowTest;
import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
public class WessbasToBenchFlowConverter {

	/**
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @param system
	 *            The system representation.
	 * @param annotation
	 *            The system annotation.
	 * @param extension
	 *            An extension of the annotation. May cause an
	 *            {@link AnnotationNotSupportedException}.
	 * @return An executable load test corresponding to the load represented by the workload model.
	 * @throws AnnotationNotSupportedException
	 *             if the passed {@link AnnotationExtension} cannot be converted to the load test.
	 */
	public BenchFlowTest convertToWorkload(WorkloadModel workloadModel, TargetSystem system, SystemAnnotation annotation, AnnotationExtension extension) throws AnnotationNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @param system
	 *            The system representation.
	 * @param annotation
	 *            The system annotation.
	 * @return An executable load test corresponding to the load represented by the workload model.
	 */
	public BenchFlowTest convertToWorkload(WorkloadModel workloadModel, TargetSystem system, SystemAnnotation annotation) {

		return null;
	}

}
