package org.continuity.workload.annotation.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationViolation {

	@JsonProperty("message")
	private AnnotationViolationType type;

	private ModelElementReference referenced;

	private boolean breaking;

	public AnnotationViolation(AnnotationViolationType type, ModelElementReference referenced, boolean breaking) {
		this.type = type;
		this.referenced = referenced;
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
