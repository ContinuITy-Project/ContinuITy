package org.continuity.workload.dsl.annotation.visitor;

import java.util.function.Predicate;

import org.continuity.workload.dsl.annotation.AnnotationElement;
import org.continuity.workload.dsl.annotation.SystemAnnotation;

/**
 * A visitor that can be used to traverse {@link SystemAnnotation}s or nested
 * {@link AnnotationElement}s.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationVisitor {

	private final Predicate<AnnotationElement> operation;

	/**
	 * Creates a new visitor. The operation is called on every single element in depth-first
	 * ordering.
	 *
	 * @param operation
	 *            The operation to be executed on the elements. It should return whether the nested
	 *            elements should be visited, as well.
	 */
	public AnnotationVisitor(Predicate<AnnotationElement> operation) {
		this.operation = operation;
	}

	/**
	 * Visits the passed element and calls the operation.
	 *
	 * @param element
	 *            The element to be visited.
	 */
	public void visit(AnnotationElement element) {
		if (operation.test(element)) {
			NestedElementExtractor extractor = NestedElementExtractor.forType(element.getClass());
			for (AnnotationElement nested : extractor.getNestedElements(element)) {
				visit(nested);
			}
		}
	}

}
