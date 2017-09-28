/**
 */
package org.continuity.workload.dsl.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an HTTP interface. That is, requests to this interface can be made by calling
 * {@code domain:port/path} with the represented method, protocol, parameters and headers.
 *
 * @author Henning Schulz
 *
 */
public class HttpInterface implements ServiceInterface {

	private static final String ENCODING_DEFAULT = "<no-encoding>";

	private String name;

	private String domain;

	private String port;

	private String path;

	private String method;

	private String encoding = ENCODING_DEFAULT;

	private String protocol;

	private List<HttpParameter> parameters;

	private List<String> headers;



	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the domain of the interface.
	 *
	 * @return The domain.
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * Sets the domain of the interface.
	 *
	 * @param domain
	 *            The domain.
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Returns the port of the interface.
	 *
	 * @return The port.
	 */
	public String getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the interface.
	 *
	 * @param port
	 *            The port.
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * Returns the path of the interface.
	 *
	 * @return The path.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Sets the path of the interface.
	 *
	 * @param path
	 *            The path.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Gets the method of the interface (e.g., {@code GET}, {@code POST}, ...).
	 *
	 * @return The method.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * Sets the method of the interface (e.g., {@code GET}, {@code POST}, ...).
	 *
	 * @param method
	 *            The method.
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Returns the encoding of the interface. If not specified, it returns {@code <no-encoding>}.
	 *
	 * @return The encoding.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Sets the encoding of the interface.
	 *
	 * @param encoding
	 *            The encoding.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns the protocol of the interface (e.g., http, https).
	 *
	 * @return The protocol.
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Sets the protocol of the interface (e.g., http, https).
	 *
	 * @param protocol
	 *            The protocol.
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Returns representations of the parameters of the interface.
	 *
	 * @return The parameters.
	 */
	public List<HttpParameter> getParameters() {
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		return parameters;
	}

	/**
	 * Sets the parameters of the interface.
	 *
	 * @param parameters
	 *            The parameters.
	 */
	public void setParameters(List<HttpParameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Returns the required headers of the interface.
	 *
	 * @return The required headers.
	 */
	public List<String> getHeaders() {
		if (headers == null) {
			headers = new ArrayList<>();
		}
		return headers;
	}

	/**
	 * Sets the required headers of the interface.
	 * 
	 * @param headers
	 *            The required headers.
	 */
	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(", domain: ");
		result.append(domain);
		result.append(", port: ");
		result.append(port);
		result.append(", path: ");
		result.append(path);
		result.append(", method: ");
		result.append(method);
		result.append(", encoding: ");
		result.append(encoding);
		result.append(", protocol: ");
		result.append(protocol);
		result.append(", headers: ");
		result.append(headers);
		result.append(')');
		return result.toString();
	}

} // HttpInterface
