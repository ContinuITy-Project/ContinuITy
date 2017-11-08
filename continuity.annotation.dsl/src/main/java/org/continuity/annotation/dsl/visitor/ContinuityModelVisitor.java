package org.continuity.annotation.dsl.visitor;

import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;

import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.ann.SystemAnnotation;

/**
 * A visitor that can be used to traverse {@link SystemAnnotation}s or nested
 * {@link ContinuityModelElement}s.
 *
 * @author Henning Schulz
 *
 */
public class ContinuityModelVisitor {

	private final Predicate<ContinuityModelElement> operation;

	private final Stack<ContinuityModelElement> currentPath = new Stack<>();

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
		if (element == null) {
			return;
		}

		currentPath.clear();
		visitRecursively(element);
	}

	private void visitRecursively(ContinuityModelElement element) {
		currentPath.push(element);

		if ((element != null) && operation.test(element)) {
			NestedElementExtractor extractor = NestedElementExtractor.forType(element.getClass());
			for (ContinuityModelElement nested : extractor.getNestedElements(element)) {
				visitRecursively(nested);
			}
		}

		currentPath.pop();
	}

	/**
	 * Gets the current path. That is, the sequence of model elements from the root to the currently
	 * visited element. To retrieve only the parent, see {@link #getCurrentParent()} <br>
	 *
	 * <b>Do not change the returned path!</b>
	 *
	 * @return The current path.
	 */
	public List<ContinuityModelElement> getCurrentPath() {
		return this.currentPath;
	}

	/**
	 * Gets the parent of the currently visited element. To retrieve the full path, see
	 * {@link #getCurrentPath()}.
	 *
	 * @return The parent.
	 */
	public ContinuityModelElement getCurrentParent() {
		if (currentPath.size() < 2) {
			return null;
		} else {
			return currentPath.get(currentPath.size() - 2);
		}
	}

}
