package org.continuity.api.entities.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationViolation {

	@JsonProperty("message")
	private AnnotationViolationType type;

	@JsonProperty("affected-element")
	private ModelElementReference affectedElement;

	private boolean breaking;

	public AnnotationViolation(AnnotationViolationType type, ModelElementReference changedElement, boolean breaking) {
		this.type = type;
		this.affectedElement = changedElement;
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
	 * Gets {@link #affectedElement}.
	 *
	 * @return {@link #affectedElement}
	 */
	public ModelElementReference getAffectedElement() {
		return this.affectedElement;
	}

	/**
	 * Sets {@link #affectedElement}.
	 *
	 * @param changedElement
	 *            New value for {@link #affectedElement}
	 */
	public void setAffectedElement(ModelElementReference changedElement) {
		this.affectedElement = changedElement;
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
