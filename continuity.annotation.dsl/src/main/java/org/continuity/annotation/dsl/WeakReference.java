package org.continuity.annotation.dsl;

import org.continuity.annotation.dsl.visitor.ContinuityModelVisitor;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * A weak reference to a {@link ContinuityModelElement}. It holds the id to the element and can hold
 * the element itself. To resolve this element via the id, call
 * {@link WeakReference#resolve(ContinuityModelElement)}.
 *
 * @author Henning Schulz
 *
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIdentityReference(alwaysAsId = true)
public class WeakReference<T extends ContinuityModelElement> {

	private final String id;

	@JsonIgnore
	private final Class<T> type;

	@JsonIgnore
	private T referred = null;

	private final ContinuityModelVisitor visitor = new ContinuityModelVisitor(this::checkAndSetElement);

	private WeakReference(String id, Class<T> type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * Creates a new WeakReference to the passed {@link ContinuityModelElement}.
	 *
	 * @param referred
	 *            The element to be referred to.
	 * @return A WeakReference to the passed element.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ContinuityModelElement> WeakReference<T> create(T referred) {
		if (referred == null) {
			throw new IllegalArgumentException("Passed element is null!");
		}

		return new WeakReference<T>(referred.getId(), (Class<T>) referred.getClass());
	}

	/**
	 * Creates a new WeakReference to the specified id with the specified type.
	 *
	 * @param type
	 *            Type of the reference.
	 * @param id
	 *            Id where the reference points to.
	 * @return A WeakReference.
	 */
	public static <T extends ContinuityModelElement> WeakReference<T> create(Class<T> type, String id) {
		if (type == null) {
			throw new IllegalArgumentException("Passed type is null!");
		}
		if (id == null) {
			throw new IllegalArgumentException("Passed id is null!");
		}

		return new WeakReference<>(id, type);
	}

	/**
	 * Creates a new WeakReference to the specified id without having a type. Consequently,
	 * resolving will not check the type.
	 *
	 * @param id
	 *            Id where the reference points to.
	 * @return A WeakReference.
	 */
	public static <T extends ContinuityModelElement> WeakReference<T> createUntyped(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Passed id is null!");
		}

		return new WeakReference<>(id, null);
	}

	/**
	 * Gets the id of the referred element.
	 *
	 * @return The referred id.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Gets the referred element, if set. To set the element, call
	 * {@link WeakReference#resolve(ContinuityModelElement)}.
	 *
	 * @return The referred element (can be {@code null}, if not yet resolved).
	 */
	public T getReferred() {
		return this.referred;
	}

	/**
	 * Resolves the referred element from the passed model, if possible and returns the reference.
	 *
	 * @param model
	 *            The model containing the referred element (can be nested).
	 * @return The resolved reference of {@code null}, if the reference wasn't found.
	 */
	public T resolve(ContinuityModelElement model) {
		if (referred == null) {
			visitor.visit(model);
		}

		return referred;
	}

	@SuppressWarnings("unchecked")
	private boolean checkAndSetElement(ContinuityModelElement referred) {
		if ((id != null) && id.equals(referred.getId())) {
			if ((type == null) || type.isAssignableFrom(referred.getClass())) {
				this.referred = (T) referred;
				return false;
			} else {
				throw new IllegalStateException("The referred element is of type " + referred.getClass() + ". Expected " + type);
			}
		} else {
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "weak ref [" + id + "]";
	}

}
