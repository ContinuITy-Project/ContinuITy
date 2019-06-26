package org.continuity.idpa.annotation.extracted;

import org.continuity.idpa.IdpaElement;

public interface ValueExtraction extends IdpaElement {

	public static final String DEFAULT_FALLBACK_VALUE = "__PREVIOUS";

	/**
	 * Gets the interface from which the value is extracted.
	 *
	 * @return The extracted interface.
	 */
	EndpointOrInput getFrom();

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
