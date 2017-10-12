package org.continuity.workload.dsl;

import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.TargetSystem;
import org.continuity.workload.dsl.visitor.ContinuityModelVisitor;

/**
 * Utility calss for extracting initial annotations from system models.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationExtractor {

	/**
	 * Extracts the annotations from the specified system model.
	 *
	 * @param system
	 *            The system model.
	 * @return The extracted annotations.
	 */
	public SystemAnnotation extractAnnotation(TargetSystem system) {
		SystemToAnnotationTransformer transformer = new SystemToAnnotationTransformer();
		ContinuityModelVisitor visitor = new ContinuityModelVisitor(transformer::onModelElementVisited);
		visitor.visit(system);
		return transformer.getExtractedAnnotation();
	}

}
