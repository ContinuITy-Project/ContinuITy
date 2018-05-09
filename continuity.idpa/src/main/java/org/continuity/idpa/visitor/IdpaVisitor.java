package org.continuity.idpa.visitor;

import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;

/**
 * A visitor that can be used to traverse {@link ApplicationAnnotation}s or nested
 * {@link IdpaElement}s.
 *
 * @author Henning Schulz
 *
 */
public class IdpaVisitor {

	private final Predicate<IdpaElement> operation;

	private final Stack<IdpaElement> currentPath = new Stack<>();

	/**
	 * Creates a new visitor. The operation is called on every single element in depth-first
	 * ordering.
	 *
	 * @param operation
	 *            The operation to be executed on the elements. It should return whether the nested
	 *            elements should be visited, as well.
	 */
	public IdpaVisitor(Predicate<IdpaElement> operation) {
		this.operation = operation;
	}

	/**
	 * Visits the passed element and calls the operation.
	 *
	 * @param element
	 *            The element to be visited.
	 */
	public void visit(IdpaElement element) {
		if (element == null) {
			return;
		}

		currentPath.clear();
		visitRecursively(element);
	}

	private void visitRecursively(IdpaElement element) {
		currentPath.push(element);

		if ((element != null) && operation.test(element)) {
			NestedElementExtractor extractor = NestedElementExtractor.forType(element.getClass());
			for (IdpaElement nested : extractor.getNestedElements(element)) {
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
	public List<IdpaElement> getCurrentPath() {
		return this.currentPath;
	}

	/**
	 * Gets the parent of the currently visited element. To retrieve the full path, see
	 * {@link #getCurrentPath()}.
	 *
	 * @return The parent.
	 */
	public IdpaElement getCurrentParent() {
		if (currentPath.size() < 2) {
			return null;
		} else {
			return currentPath.get(currentPath.size() - 2);
		}
	}

}
