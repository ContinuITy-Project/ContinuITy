package org.continuity.commons.idpa;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.visitor.IdpaVisitor;

/**
 * Utility calss for extracting initial annotations from application models.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationExtractor {

	/**
	 * Extracts the annotations from the specified application model.
	 *
	 * @param application
	 *            The application model.
	 * @return The extracted annotations.
	 */
	public ApplicationAnnotation extractAnnotation(Application application) {
		ApplicationToAnnotationTransformer transformer = new ApplicationToAnnotationTransformer();
		IdpaVisitor visitor = new IdpaVisitor(transformer::onModelElementVisited);
		visitor.visit(application);
		return transformer.getExtractedAnnotation();
	}

}
