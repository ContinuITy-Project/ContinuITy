package org.continuity.idpa.visitor;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.continuity.idpa.IdpaElement;

/**
 * Can be used to find an element meeting a custom condition within a ContinuITy model. Can either
 * be used as operation of {@link IdpaByClassSearcher} or directly.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of the model element to be found.
 */
public class FindBy<T extends IdpaElement> implements Consumer<T> {

	private final Predicate<T> condition;

	private final Class<T> type;

	private T found;

	public FindBy(Predicate<T> condition, Class<T> type) {
		this.condition = condition;
		this.type = type;
	}

	/**
	 * Utility method. To be used as follows: <br>
	 * <code>Parameter param = FindBy.find(myCondition, Parameter.class).in(model).getFound();</code>
	 *
	 * @param condition
	 *            The condition to be applied.
	 * @param type
	 *            The type of the element to be found.
	 * @return A created instance of {@link FindBy}.
	 */
	public static <T extends IdpaElement> FindBy<T> find(Predicate<T> condition, Class<T> type) {
		return new FindBy<>(condition, type);
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
	public static <T extends IdpaElement> FindBy<T> findById(String id, Class<T> type) {
		return new FindBy<>(elem -> Objects.equals(id, elem.getId()), type);
	}

	/**
	 * Specifies where to search for the id. To be used as follows: <br>
	 * <code>Parameter param = FindById.find(myId, Parameter.class).in(oldModel).getFound();</code>
	 *
	 * @param element
	 *            The model element to search in.
	 * @return The same instance of {@link FindById}.
	 */
	public FindBy<T> in(IdpaElement element) {
		IdpaByClassSearcher<T> searcher = new IdpaByClassSearcher<>(type, this::accept);
		searcher.visit(element);
		return this;
	}

	@Override
	public void accept(T t) {
		if ((found == null) && condition.test(t)) {
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
