package org.continuity.idpa.application.entities;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.WeakReference;

/**
 * @author Henning Schulz
 *
 */
public class ModelElementReference {

	private static final String UNKNOWN_TYPE = "UNKNOWN";

	private String type;

	private String id;

	/**
	 *
	 */
	public ModelElementReference() {
	}

	public ModelElementReference(IdpaElement element) {
		this(element.getClass().getSimpleName(), element.getId());
	}

	public ModelElementReference(WeakReference<?> ref) {
		this(ref.getType() == null ? UNKNOWN_TYPE : ref.getType().getSimpleName(), ref.getId());
	}

	/**
	 * @param type
	 * @param id
	 */
	public ModelElementReference(String type, String id) {
		super();
		this.type = type;
		this.id = id;
	}

	/**
	 * Gets {@link #type}.
	 *
	 * @return {@link #type}
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets {@link #type}.
	 *
	 * @param type
	 *            New value for {@link #type}
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets {@link #id}.
	 *
	 * @return {@link #id}
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets {@link #id}.
	 *
	 * @param id
	 *            New value for {@link #id}
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id, type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ModelElementReference)) {
			return false;
		}

		ModelElementReference other = (ModelElementReference) obj;
		return StringUtils.equals(this.id, other.id) && StringUtils.equals(this.type, other.type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return id + " [" + type + "]";
	}

}
