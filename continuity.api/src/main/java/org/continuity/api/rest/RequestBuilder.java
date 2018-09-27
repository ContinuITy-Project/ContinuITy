package org.continuity.api.rest;

/**
 * A builder for REST requests.
 *
 * @author Henning Schulz
 *
 */
public class RequestBuilder {

	private String host;

	private final String path;

	private final StringBuilder queryString = new StringBuilder();
	private boolean queryStringEmpty = true;

	private boolean includeProtocol = true;

	/**
	 * Constructor.
	 *
	 * @param host
	 *            The host of the request.
	 * @param path
	 *            The path of the request.
	 */
	public RequestBuilder(String host, String path) {
		this.host = host;
		this.path = path;
	}

	/**
	 * Gets the currently represented request.
	 *
	 * @return The request as string.
	 */
	public String get() {
		String protocol = "";

		if (includeProtocol && !host.startsWith("http://") && !host.startsWith("https://")) {
			protocol = "http://";
		}

		return protocol + host + path + queryString;
	}

	/**
	 * Adds a query to the request, e.g., <code>?foo=bar</code> in <code>/my/path?foo=bar</code>.
	 * Only adds one <code>parameter=value</code> pair. Further pairs can be added by subsequent
	 * calls.
	 *
	 * @param param
	 *            The parameter name.
	 * @param value
	 *            The value of the parameter.
	 * @return The builder for further request modifications.
	 */
	public RequestBuilder withQuery(String param, String value) {
		if (queryStringEmpty) {
			queryString.append("?");
			queryStringEmpty = false;
		} else {
			queryString.append("&");
		}

		queryString.append(param);
		queryString.append("=");
		queryString.append(value);

		return this;
	}

	/**
	 * Replaces the stored host name.
	 *
	 * @param host
	 *            The new host name.
	 * @return The builder for further request modifications.
	 */
	public RequestBuilder withHost(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Omits the host and the protocol at the beginning of the request.
	 *
	 * @return URI
	 */
	public String getURI() {
		return path + queryString;
	}
	
	/**
	 * Omits the {@code http://} at the beginning of the request.
	 *
	 * @return The builder for further request modifications.
	 */
	public RequestBuilder withoutProtocol() {
		this.includeProtocol = false;
		return this;
	}

	@Override
	public String toString() {
		return get();
	}

}
