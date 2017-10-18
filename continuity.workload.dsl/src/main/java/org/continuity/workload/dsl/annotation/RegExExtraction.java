/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.AbstractContinuityModelElement;
import org.continuity.workload.dsl.WeakReference;
import org.continuity.workload.dsl.system.ServiceInterface;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an extraction of a value specified by a regular expression from the response of an
 * interface. The regular expression can specify one ore several groups that are to be extracted.
 * The template specifies how these groups should be combined. Finally, if there are several
 * matches, the matchNumber defines which one to take.
 *
 * @author Henning Schulz
 *
 */
public class RegExExtraction extends AbstractContinuityModelElement {

	private static final String DEFAULT_RESPONSE_KEY = "<default>";
	private static final String DEFAULT_FALLBACK_VALUE = "NOT FOUND";
	private static final String DEFAULT_TEMPLATE = "(1)";
	private static final int DEFAULT_MATCH_NUMBER = 1;

	private String pattern;

	@JsonProperty(value = "from")
	private WeakReference<ServiceInterface<?>> from;

	@JsonProperty(value = "response-key")
	@JsonInclude(value = Include.CUSTOM, valueFilter = ResponseKeyValueFilter.class)
	private String responseKey = DEFAULT_RESPONSE_KEY;

	@JsonProperty(value = "fallback")
	@JsonInclude(value = Include.CUSTOM, valueFilter = FallbackValueFilter.class)
	private String fallbackValue = DEFAULT_FALLBACK_VALUE;

	@JsonProperty(value = "template")
	@JsonInclude(value = Include.CUSTOM, valueFilter = TemplateValueFilter.class)
	private String template = DEFAULT_TEMPLATE;

	@JsonProperty(value = "match-number")
	@JsonInclude(value = Include.CUSTOM, valueFilter = MatchNumberValueFilter.class)
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
	public WeakReference<ServiceInterface<?>> getFrom() {
		return this.from;
	}

	/**
	 * Sets the interface from which the value is extracted.
	 *
	 * @param extracted
	 *            The extracted interface.
	 */
	public void setFrom(WeakReference<ServiceInterface<?>> from) {
		this.from = from;
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
	public void setResponseKey(String responseKey) {
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

	private static final class ResponseKeyValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return DEFAULT_RESPONSE_KEY.equals(obj);
		}

	}

	private static final class FallbackValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return DEFAULT_FALLBACK_VALUE.equals(obj);
		}

	}

	private static final class TemplateValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return DEFAULT_TEMPLATE.equals(obj);
		}

	}

	private static final class MatchNumberValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return (obj != null) && obj.equals(DEFAULT_MATCH_NUMBER);
		}

	}

}
