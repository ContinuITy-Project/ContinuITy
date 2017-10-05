package org.continuity.workload.dsl.annotation;

/**
 * An AnnotationElement optionally holds an id that can be used to reference it from outside.
 *
 * @author Henning Schulz
 *
 */
public interface AnnotationElement {

	/**
	 * Returns an id that identifies this element within the annotation.
	 *
	 * @return The id.
	 */
	String getId();

	/**
	 * Returns whether the element holds an id.
	 *
	 * @return {@code true} if the elements holds an id or {@code false} if not.
	 */
	boolean hasId();

}
