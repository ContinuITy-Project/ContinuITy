package org.continuity.idpa;

import org.continuity.idpa.serialization.WeakReferenceDeserializer;
import org.continuity.idpa.visitor.IdpaVisitor;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A weak reference to a {@link IdpaElement}. It holds the id to the element and can hold
 * the element itself. To resolve this element via the id, call
 * {@link WeakReference#resolve(IdpaElement)}.
 *
 * @author Henning Schulz
 *
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIdentityReference(alwaysAsId = true)
@JsonDeserialize(using = WeakReferenceDeserializer.class)
public class WeakReference<T extends IdpaElement> {

	private final String id;

	@JsonIgnore
	private final Class<T> type;

	@JsonIgnore
	private T referred = null;

	private final IdpaVisitor visitor = new IdpaVisitor(this::checkAndSetElement);

	private WeakReference(String id, Class<T> type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * Creates a new WeakReference to the passed {@link IdpaElement}.
	 *
	 * @param referred
	 *            The element to be referred to.
	 * @return A WeakReference to the passed element.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IdpaElement> WeakReference<T> create(T referred) {
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
	public static <T extends IdpaElement> WeakReference<T> create(Class<T> type, String id) {
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
	public static <T extends IdpaElement> WeakReference<T> createUntyped(String id) {
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
	 * {@link WeakReference#resolve(IdpaElement)}.
	 *
	 * @return The referred element (can be {@code null}, if not yet resolved).
	 */
	public T getReferred() {
		return this.referred;
	}

	/**
	 * Gets the type of the reference, if present.
	 *
	 * @return The type or {@code null}, if not present.
	 */
	public Class<T> getType() {
		return this.type;
	}

	/**
	 * Resolves the referred element from the passed model, if possible and returns the reference.
	 * Overwrites an already resolved element - potentially with {@code null}.
	 *
	 * @param model
	 *            The model containing the referred element (can be nested).
	 * @return The resolved reference of {@code null}, if the reference wasn't found.
	 */
	public T resolve(IdpaElement model) {
		referred = null;
		visitor.visit(model);
		return referred;
	}

	/**
	 * Returns whether the referred element has already been resolved. That is, if the element is
	 * not {@code null}.
	 *
	 *
	 * @return {@code true} if the referred element has been resolved of {@code false}, otherwise.
	 */
	@JsonIgnore
	public boolean isResolved() {
		return referred != null;
	}

	@SuppressWarnings("unchecked")
	private boolean checkAndSetElement(IdpaElement referred) {
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
