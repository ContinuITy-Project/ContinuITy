package org.continuity.workload.dsl.visitor;

import java.util.function.Predicate;

import org.continuity.workload.dsl.ContinuityModelElement;
import org.continuity.workload.dsl.annotation.SystemAnnotation;

/**
 * A visitor that can be used to traverse {@link SystemAnnotation}s or nested
 * {@link ContinuityModelElement}s.
 *
 * @author Henning Schulz
 *
 */
public class ContinuityModelVisitor {

	private final Predicate<ContinuityModelElement> operation;

	/**
	 * Creates a new visitor. The operation is called on every single element in depth-first
	 * ordering.
	 *
	 * @param operation
	 *            The operation to be executed on the elements. It should return whether the nested
	 *            elements should be visited, as well.
	 */
	public ContinuityModelVisitor(Predicate<ContinuityModelElement> operation) {
		this.operation = operation;
	}

	/**
	 * Visits the passed element and calls the operation.
	 *
	 * @param element
	 *            The element to be visited.
	 */
	public void visit(ContinuityModelElement element) {
		if ((element != null) && operation.test(element)) {
			NestedElementExtractor extractor = NestedElementExtractor.forType(element.getClass());
			for (ContinuityModelElement nested : extractor.getNestedElements(element)) {
				visit(nested);
			}
		}
	}

}
