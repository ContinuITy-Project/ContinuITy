package org.continuity.session.logs.extractor;

import java.util.Map;
import java.util.Optional;

/**
 * Generic data wrapper. Provides all necessary data in order to create a session log
 *
 * @author Tobias Angerstein
 *
 */
public interface HTTPRequestData {
	/**
	 * Returns unique identifier
	 *
	 * @return identifier
	 */
	public String getIdentifier();

	/**
	 * Returns timestamp in nanos
	 *
	 * @return timestamp
	 */
	public long getTimestamp();

	/**
	 * Returns response time in nanos
	 *
	 * @return response time
	 */
	public long getResponseTime();

	/**
	 * Returns uri of request
	 *
	 * @return URI
	 */
	public String getUri();

	/**
	 * Returns {@link Optional} of a HTTPParameters
	 *
	 * @return HTTPParameters
	 */
	public Optional<Map<String, String[]>> getHTTPParameters();

	/**
	 * Returns {@link Optional} of a request body
	 *
	 * @return request body
	 */
	public Optional<String> getRequestBody();

	/**
	 * Returns port
	 *
	 * @return port
	 */
	public int getPort();

	/**
	 * Returns host
	 *
	 * @return host
	 */
	public String getHost();

	/**
	 * Returns request method
	 *
	 * @return request method
	 */
	public String getRequestMethod();

	/**
	 * Returns response code of delivered response
	 *
	 * @return response code
	 */
	public long getResponseCode();

	/**
	 * Returns the session ID
	 *
	 * @return session ID
	 */
	public String getSessionId();

	/**
	 * Returns the business transaction
	 *
	 * @return business transaction
	 */
	public String getBusinessTransaction();

	/**
	 * Indicates whether this request is special and thus, must not be labeled by mapping to an
	 * application model. Instead, the built-in business transaction should be used.
	 *
	 * @return {@code true} if this request is special
	 */
	public default boolean isSpecial() {
		return false;
	}
}
