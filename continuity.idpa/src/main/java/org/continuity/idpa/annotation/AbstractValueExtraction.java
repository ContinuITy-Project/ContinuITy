package org.continuity.idpa.annotation;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.application.Endpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AbstractValueExtraction extends AbstractIdpaElement implements ValueExtraction {

	private static final String DEFAULT_RESPONSE_KEY = "<default>";
	private static final String DEFAULT_FALLBACK_VALUE = "NOT FOUND";
	private static final int DEFAULT_MATCH_NUMBER = 1;

	@JsonProperty(value = "from")
	private WeakReference<Endpoint<?>> from;

	@JsonProperty(value = "response-key")
	@JsonInclude(value = Include.CUSTOM, valueFilter = ResponseKeyValueFilter.class)
	private String responseKey = DEFAULT_RESPONSE_KEY;

	@JsonProperty(value = "fallback")
	@JsonInclude(value = Include.CUSTOM, valueFilter = FallbackValueFilter.class)
	private String fallbackValue = DEFAULT_FALLBACK_VALUE;

	@JsonProperty(value = "match-number")
	@JsonInclude(value = Include.CUSTOM, valueFilter = MatchNumberValueFilter.class)
	private int matchNumber = DEFAULT_MATCH_NUMBER;

	/**
	 * Gets the interface from which the value is extracted.
	 *
	 * @return The extracted interface.
	 */
	@Override
	public WeakReference<Endpoint<?>> getFrom() {
		return this.from;
	}

	/**
	 * Sets the interface from which the value is extracted.
	 *
	 * @param extracted
	 *            The extracted interface.
	 */
	public void setFrom(WeakReference<Endpoint<?>> from) {
		this.from = from;
	}

	/**
	 * Gets the key. Can be used to specify a specific response, e.g., the header or body of an HTTP
	 * response.
	 *
	 * @return {@link #key} The key.
	 */
	@Override
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
	@Override
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
	 * Gets the match number. This number specifies which one of possibly several matches should be
	 * taken. 0 means to take a random one. -1 means to take all.
	 *
	 * @return The match number.
	 */
	@Override
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
		ObjectMapper mapper = new ObjectMapper();

		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "Cannot transform to string: " + e;
		}
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
