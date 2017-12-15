package org.continuity.frontend.entities;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationViolation {

	private String message;

	private ModelElementReference referenced;

	private boolean breaking;

	/**
	 * Gets {@link #message}.
	 *
	 * @return {@link #message}
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Sets {@link #message}.
	 *
	 * @param message
	 *            New value for {@link #message}
	 */
	public void setMessage(String message) {
		this.message = message;
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
	public void setReferenced(ModelElementReference referenced) {
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
