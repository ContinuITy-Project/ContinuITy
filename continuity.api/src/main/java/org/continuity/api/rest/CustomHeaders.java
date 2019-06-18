package org.continuity.api.rest;

/**
 * Collection of custom headers used in the scope of this project.
 *
 * @author Henning Schulz
 *
 */
public class CustomHeaders {

	/**
	 * Marks that an IDPA annotation is broken with respect to the corresponding application model.
	 */
	public static final String BROKEN = "X-Continuity-Broken";

	private CustomHeaders() {
	}

}
