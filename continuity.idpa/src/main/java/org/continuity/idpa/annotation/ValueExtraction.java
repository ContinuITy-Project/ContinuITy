package org.continuity.idpa.annotation;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.application.Endpoint;

public interface ValueExtraction extends IdpaElement {

	/**
	 * Gets the interface from which the value is extracted.
	 *
	 * @return The extracted interface.
	 */
	WeakReference<Endpoint<?>> getFrom();

	/**
	 * Gets the key. Can be used to specify a specific response, e.g., the header or body of an HTTP
	 * response.
	 *
	 * @return {@link #key} The key.
	 */
	String getResponseKey();

	/**
	 * Gets the value that is to be used if there was no match.
	 *
	 * @return the fallback value.
	 */
	String getFallbackValue();

	/**
	 * Gets the match number. This number specifies which one of possibly several matches should be
	 * taken. 0 means to take a random one. -1 means to take all.
	 *
	 * @return The match number.
	 */
	int getMatchNumber();

}
