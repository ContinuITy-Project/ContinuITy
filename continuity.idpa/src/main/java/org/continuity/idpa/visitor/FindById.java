package org.continuity.idpa.visitor;

import java.util.Objects;
import java.util.function.Consumer;

import org.continuity.idpa.IdpaElement;

/**
 * Can be used to find an element by ID within a ContinuITy model. Can either be used as operation
 * of {@link IdpaByClassSearcher} or directly.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of the model element to be found.
 */
public class FindById<T extends IdpaElement> implements Consumer<T> {

	private final String id;

	private final Class<T> type;

	private T found;

	public FindById(String id, Class<T> type) {
		this.id = id;
		this.type = type;
	}

	public FindById(String id) {
		this(id, null);
	}

	/**
	 * Utility method. To be used as follows: <br>
	 * <code>Parameter param = FindById.find(myId, Parameter.class).in(oldModel).getFound();</code>
	 *
	 * @param id
	 *            The id of the element to be found.
	 * @param type
	 *            The type of the element to be found.
	 * @return A created instance of {@link FindById}.
	 */
	public static <T extends IdpaElement> FindById<T> find(String id, Class<T> type) {
		return new FindById<>(id, type);
	}

	/**
	 * Specifies where to search for the id. To be used as follows: <br>
	 * <code>Parameter param = FindById.find(myId, Parameter.class).in(oldModel).getFound();</code>
	 *
	 * @param element
	 *            The model element to search in.
	 * @return The same instance of {@link FindById}.
	 */
	public FindById<T> in(IdpaElement element) {
		IdpaByClassSearcher<T> searcher = new IdpaByClassSearcher<>(type, this::accept);
		searcher.visit(element);
		return this;
	}

	@Override
	public void accept(T t) {
		if ((found == null) && Objects.equals(id, t.getId())) {
			found = t;
		}
	}

	/**
	 * Returns the found element.
	 *
	 * @return The found element or {@code null}, if nothing was found.
	 */
	public T getFound() {
		return found;
	}

}
