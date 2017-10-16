package org.continuity.workload.driver;

import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtension;
import org.continuity.workload.dsl.system.TargetSystem;

/**
 * A WorkloadConverter is responsible to convert a workload model to an executable load test taking
 * annotations into account.
 *
 * @author Henning Schulz
 *
 * @param <W>
 *            Type of the workload model
 * @param <T>
 *            Type of the executable load test
 */
public interface WorkloadConverter<W, T> {

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
	T convertToWorkload(W workloadModel, TargetSystem system, SystemAnnotation annotation, AnnotationExtension extension) throws AnnotationNotSupportedException;

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
	T convertToWorkload(W workloadModel, TargetSystem system, SystemAnnotation annotation);

}
