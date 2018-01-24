package org.continuity.system.model.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class SystemChange {

	@JsonProperty("message")
	private SystemChangeType type;

	private ModelElementReference referenced;

	public SystemChange(SystemChangeType type, ModelElementReference referenced) {
		this.type = type;
		this.referenced = referenced;
	}

	public SystemChange(SystemChangeType type) {
		this(type, null);
	}

	public SystemChange() {
	}

	/**
	 * Gets {@link #type}.
	 *
	 * @return {@link #type}
	 */
	public SystemChangeType getType() {
		return this.type;
	}

	/**
	 * Gets {@link #referenced}.
	 *
	 * @return {@link #referenced}
	 */
	public ModelElementReference getReferenced() {
		return this.referenced;
	}

	/**
	 * Sets {@link #referenced}.
	 *
	 * @param referenced
	 *            New value for {@link #referenced}
	 */
	public void setReferencedId(ModelElementReference referenced) {
		this.referenced = referenced;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return type + ": " + referenced;
	}

}
