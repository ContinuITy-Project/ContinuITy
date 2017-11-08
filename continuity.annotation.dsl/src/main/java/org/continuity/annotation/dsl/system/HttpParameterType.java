/**
 */
package org.continuity.annotation.dsl.system;

/**
 * The type of an HTTP parameter.
 *
 * @author Henning Schulz
 *
 */
public enum HttpParameterType {

	/**
	 * Normal request parameter.
	 */
	REQ_PARAM,

	/**
	 * The request body.
	 */
	BODY,

	/**
	 * Part of the URL.
	 */
	URL_PART

}
