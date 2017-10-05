/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.system.ServiceInterface;

/**
 * Represents an extraction of a value specified by a regular expression from the response of an
 * interface. The regular expression can specify one ore several groups that are to be extracted.
 * The template specifies how these groups should be combined. Finally, if there are several
 * matches, the matchNumber defines which one to take.
 *
 * @author Henning Schulz
 *
 */
public class RegExExtraction extends AbstractAnnotationElement {

	private static final String DEFAULT_RESPONSE_KEY = "<default>";
	private static final String DEFAULT_FALLBACK_VALUE = "NOT FOUND";
	private static final String DEFAULT_TEMPLATE = "(1)";
	private static final int DEFAULT_MATCH_NUMBER = 1;

	private String pattern;

	private ServiceInterface extracted;

	private String responseKey = DEFAULT_RESPONSE_KEY;

	private String fallbackValue = DEFAULT_FALLBACK_VALUE;

	private String template = DEFAULT_TEMPLATE;

	private int matchNumber = DEFAULT_MATCH_NUMBER;

	/**
	 * Gets the pattern used to extract the value.
	 *
	 * @return The pattern.
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * Sets the pattern used to extract the value.
	 *
	 * @param pattern
	 *            The pattern.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Gets the interface from which the value is extracted.
	 *
	 * @return The extracted interface.
	 */
	public ServiceInterface getExtracted() {
		return this.extracted;
	}

	/**
	 * Sets the interface from which the value is extracted.
	 *
	 * @param extracted
	 *            The extracted interface.
	 */
	public void setExtracted(ServiceInterface extracted) {
		this.extracted = extracted;
	}

	/**
	 * Gets the key. Can be used to specify a specific response, e.g., the header or body of an HTTP
	 * response.
	 *
	 * @return {@link #key} The key.
	 */
	public String getResponseKey() {
		return this.responseKey;
	}

	/**
	 * Sets the key. Can be used to specify a specific response, e.g., the header or body of an HTTP
	 * response.
	 *
	 * @param key
	 *            The key.
	 */
	public void setResponseKeyey(String responseKey) {
		this.responseKey = responseKey;
	}

	/**
	 * Gets the value that is to be used if there was no match.
	 *
	 * @return the fallback value.
	 */
	public String getFallbackValue() {
		return this.fallbackValue;
	}

	/**
	 * Sets the value that is to be used if there was no match.
	 *
	 * @param notFoundValue
	 *            The new fallback value.
	 */
	public void setFallbackValue(String notFoundValue) {
		this.fallbackValue = notFoundValue;
	}

	/**
	 * Gets the template. An extracted group with number {@code n} is referenced by {@code (n)}.
	 *
	 * @return The template.
	 */
	public String getTemplate() {
		return this.template;
	}

	/**
	 * Sets the template. An extracted group with number {@code n} can be referenced by {@code (n)}.
	 *
	 * @param template
	 *            The new template.
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Gets the match number. This number specifies which one of possibly several matches should be
	 * taken. 0 means to take a random one. -1 means to take all.
	 *
	 * @return The match number.
	 */
	public int getMatchNumber() {
		return this.matchNumber;
	}

	/**
	 * Sets the match number. This number specifies which one of possibly several matches should be
	 * taken. 0 means to take a random one. -1 means to take all.
	 *
	 * @param matchNumber
	 *            The new match number.
	 */
	public void setMatchNumber(int matchNumber) {
		this.matchNumber = matchNumber;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (pattern: ");
		result.append(pattern);
		result.append(", responseKey: ");
		result.append(responseKey);
		result.append(')');
		return result.toString();
	}

}
