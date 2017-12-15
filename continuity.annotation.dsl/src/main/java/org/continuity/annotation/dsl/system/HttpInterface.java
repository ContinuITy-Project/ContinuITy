/**
 */
package org.continuity.annotation.dsl.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents an HTTP interface. That is, requests to this interface can be made by calling
 * {@code domain:port/path} with the represented method, protocol, parameters and headers.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "domain", "port", "path", "method", "encoding", "headers", "parameters" })
public class HttpInterface extends AbstractContinuityModelElement implements ServiceInterface<HttpParameter> {

	private static final String DEFAULT_ENCODING = "<no-encoding>";

	private String domain;

	private String port;

	private String path;

	private String method;

	@JsonInclude(value = Include.CUSTOM, valueFilter = EncodingValueFilter.class)
	private String encoding = DEFAULT_ENCODING;

	private String protocol;

	@JsonInclude(Include.NON_EMPTY)
	private List<HttpParameter> parameters;

	@JsonInclude(Include.NON_EMPTY)
	private List<String> headers;

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
	@Override
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
		result.append(" (id: ");
		result.append(getId());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		if (!this.getClass().getName().equals(obj.getClass().getName())) {
			return false;
		}

		HttpInterface other = (HttpInterface) obj;

		boolean equal = Objects.equals(this.domain, other.domain);
		equal = equal && Objects.equals(this.port, other.port);
		equal = equal && Objects.equals(this.path, other.path);
		equal = equal && Objects.equals(this.method, other.method);
		equal = equal && Objects.equals(this.encoding, other.encoding);
		equal = equal && Objects.equals(this.protocol, other.protocol);

		Collections.sort(this.getParameters());
		Collections.sort(other.getParameters());
		equal = equal && this.getParameters().equals(other.getParameters());

		Collections.sort(this.getHeaders());
		Collections.sort(other.getHeaders());
		equal = equal && this.getHeaders().equals(other.getHeaders());

		return equal;
	}

	private static class EncodingValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return DEFAULT_ENCODING.equals(obj);
		}

	}

}
