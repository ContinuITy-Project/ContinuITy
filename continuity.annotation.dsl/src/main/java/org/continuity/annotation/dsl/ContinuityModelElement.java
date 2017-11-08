package org.continuity.annotation.dsl;

/**
 * An AnnotationElement optionally holds an id that can be used to reference it from outside.
 *
 * @author Henning Schulz
 *
 */
public interface ContinuityModelElement {

	/**
	 * Returns an id that identifies this element within the annotation.
	 *
	 * @return The id.
	 */
	String getId();

	/**
	 * Sets the id that identifies this element within the annotation.
	 *
	 * @param id
	 *            The new value for id.
	 */
	void setId(String id);

	/**
	 * Returns whether the element holds an id.
	 *
	 * @return {@code true} if the elements holds an id or {@code false} if not.
	 */
	boolean hasId();

}
