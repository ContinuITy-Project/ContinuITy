package org.continuity.commons.exceptions;

import org.continuity.annotation.dsl.custom.CustomAnnotation;

/**
 * Exception signaling that a passed {@link CustomAnnotation} is not supported by the load
 * driver.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationNotSupportedException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -7065508762889434696L;

	/**
	 * Default constructor.
	 */
	public AnnotationNotSupportedException() {
		super();
	}

	/**
	 * Constructor consuming message.
	 *
	 * @param message
	 *            Message to be printed.
	 */
	public AnnotationNotSupportedException(String message) {
		super(message);
	}

}
