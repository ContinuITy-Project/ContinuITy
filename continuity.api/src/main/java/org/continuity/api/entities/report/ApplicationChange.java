package org.continuity.api.entities.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class ApplicationChange {

	@JsonProperty("message")
	private ApplicationChangeType type;

	@JsonProperty("changed-element")
	private ModelElementReference changedElement;

	@JsonProperty("changed-property")
	@JsonInclude(Include.NON_NULL)
	private String changedProperty;

	public ApplicationChange(ApplicationChangeType type, ModelElementReference referenced, String changedProperty) {
		this.type = type;
		this.changedElement = referenced;
		this.changedProperty = changedProperty;
	}

	public ApplicationChange(ApplicationChangeType type, ModelElementReference referenced) {
		this(type, referenced, null);
	}

	public ApplicationChange(ApplicationChangeType type) {
		this(type, null);
	}

	public ApplicationChange() {
	}

	/**
	 * Gets {@link #type}.
	 *
	 * @return {@link #type}
	 */
	public ApplicationChangeType getType() {
		return this.type;
	}

	/**
	 * Gets {@link #changedElement}.
	 *
	 * @return {@link #changedElement}
	 */
	public ModelElementReference getChangedElement() {
		return this.changedElement;
	}

	/**
	 * Sets {@link #changedElement}.
	 *
	 * @param changedElement
	 *            New value for {@link #changedElement}
	 */
	public void setChangedElement(ModelElementReference changedElement) {
		this.changedElement = changedElement;
	}

	/**
	 * Gets {@link #changedProperty}.
	 *
	 * @return {@link #changedProperty}
	 */
	public String getChangedProperty() {
		return this.changedProperty;
	}

	/**
	 * Sets {@link #changedProperty}.
	 *
	 * @param changedProperty
	 *            New value for {@link #changedProperty}
	 */
	public void setChangedProperty(String changedProperty) {
		this.changedProperty = changedProperty;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type);
		builder.append(":");
		builder.append(changedElement);

		if (changedProperty != null) {
			builder.append("(");
			builder.append(changedProperty);
			builder.append(")");
		}

		return builder.toString();
	}

}
