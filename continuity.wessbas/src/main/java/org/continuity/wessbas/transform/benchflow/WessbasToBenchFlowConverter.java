package org.continuity.wessbas.transform.benchflow;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.custom.CustomAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.exceptions.AnnotationNotSupportedException;

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
	 *             if the passed {@link CustomAnnotation} cannot be converted to the load test.
	 */
	public Object convertToWorkload(WorkloadModel workloadModel, SystemModel system, SystemAnnotation annotation, CustomAnnotation extension) throws AnnotationNotSupportedException {
		// cloud.benchflow.dsl.definition.BenchFlowTest
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
	public Object convertToWorkload(WorkloadModel workloadModel, SystemModel system, SystemAnnotation annotation) {
		// cloud.benchflow.dsl.definition.BenchFlowTest
		return null;
	}

}
