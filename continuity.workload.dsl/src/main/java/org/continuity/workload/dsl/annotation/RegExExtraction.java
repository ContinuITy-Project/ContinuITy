/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.system.ServiceInterface;

/**
 * Represents an extraction of a value specified by a regular expression from the response of an
 * interface.
 *
 * @author Henning Schulz
 *
 */
public class RegExExtraction {

	private static final String DEFAULT_KEY = "<default>";

	private String pattern;

	private ServiceInterface extracted;

	private String key = DEFAULT_KEY;

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
	public String getKey() {
		return this.key;
	}

	/**
	 * Sets the key. Can be used to specify a specific response, e.g., the header or body of an HTTP
	 * response.
	 *
	 * @param key
	 *            The key.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (pattern: ");
		result.append(pattern);
		result.append(", key: ");
		result.append(key);
		result.append(')');
		return result.toString();
	}

}
