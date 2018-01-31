package org.continuity.system.annotation.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationViolation {

	@JsonProperty("message")
	private AnnotationViolationType type;

	@JsonProperty("changed-element")
	private ModelElementReference changedElement;

	private boolean breaking;

	public AnnotationViolation(AnnotationViolationType type, ModelElementReference changedElement, boolean breaking) {
		this.type = type;
		this.changedElement = changedElement;
		this.breaking = breaking;
	}

	public AnnotationViolation(AnnotationViolationType type, ModelElementReference referenced) {
		this(type, referenced, type.isBreaking());
	}

	public AnnotationViolation(AnnotationViolationType type) {
		this(type, null);
	}

	public AnnotationViolation() {
	}

	/**
	 * Gets {@link #type}.
	 *
	 * @return {@link #type}
	 */
	public AnnotationViolationType getType() {
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
	 * Gets {@link #breaking}.
	 *
	 * @return {@link #breaking}
	 */
	public boolean isBreaking() {
		return this.breaking;
	}

	/**
	 * Sets {@link #breaking}.
	 *
	 * @param breaking
	 *            New value for {@link #breaking}
	 */
	public void setBreaking(boolean breaking) {
		this.breaking = breaking;
	}

}
