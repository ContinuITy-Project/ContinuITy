package org.continuity.workload.dsl.visitor;

import java.util.function.Predicate;

import org.continuity.workload.dsl.ContinuityModelElement;

/**
 * Visitor for {@link ContinuityModelElement}s of a specific type.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of {@link ContinuityModelElement}.
 */
public class ContinuityByClassSearcher<T extends ContinuityModelElement> {

	private final Class<T> type;

	private final Predicate<T> operation;

	private final ContinuityModelVisitor visitor = new ContinuityModelVisitor(this::onElementFound);

	/**
	 * Creates a new searcher. The operation is called on every single element of the specified type
	 * in depth-first ordering.
	 *
	 * @param type
	 *            The type of the elements to be found.
	 * @param operation
	 *            The operation to be executed on the elements. It should return whether the nested
	 *            elements should be visited, as well.
	 */
	public ContinuityByClassSearcher(Class<T> type, Predicate<T> operation) {
		this.operation = operation;
		this.type = type;
	}

	/**
	 * Visits the passed model and calls the operation.
	 *
	 * @param model
	 *            The model to be visited.
	 */
	public void visit(ContinuityModelElement model) {
		visitor.visit(model);
	}

	@SuppressWarnings("unchecked")
	private boolean onElementFound(ContinuityModelElement element) {
		if (type.isAssignableFrom(element.getClass())) {
			operation.test((T) element);
		}

		return true;
	}

}
