package org.continuity.idpa.visitor;

import java.util.function.Consumer;

import org.continuity.idpa.IdpaElement;

/**
 * Visitor for {@link IdpaElement}s of a specific type.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of {@link IdpaElement}.
 */
public class IdpaByClassSearcher<T extends IdpaElement> {

	private final Class<T> type;

	private final Consumer<T> operation;

	private final IdpaVisitor visitor = new IdpaVisitor(this::onElementFound);

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
	public IdpaByClassSearcher(Class<T> type, Consumer<T> operation) {
		this.operation = operation;
		this.type = type;
	}

	/**
	 * Visits the passed model and calls the operation.
	 *
	 * @param model
	 *            The model to be visited.
	 */
	public void visit(IdpaElement model) {
		visitor.visit(model);
	}

	@SuppressWarnings("unchecked")
	private boolean onElementFound(IdpaElement element) {
		if (type.isAssignableFrom(element.getClass())) {
			operation.accept((T) element);
		}

		return true;
	}

}
