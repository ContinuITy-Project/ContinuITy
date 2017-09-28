package org.continuity.workload.dsl;

import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.TargetSystem;

/**
 * Transforms a workload model into the ContinuITy workload DSL.
 *
 * @author Henning Schulz
 *
 */
public interface DslExtractor {

	/**
	 * Transforms the workload model into a System model.
	 * 
	 * @return
	 */
	TargetSystem extractSystemModel();

	/**
	 * Generates an initial annotation that can be changes by users.
	 *
	 * @return
	 */
	SystemAnnotation extractInitialAnnotation();

}
