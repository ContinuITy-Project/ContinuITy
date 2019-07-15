package org.continuity.api.entities.artifact.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Adds the information required to generate extensive session logs to a request in a session.
 *
 * @see SessionRequest#getExtendedInformation()
 *
 * @author Henning Schulz
 *
 */
@JsonView(SessionView.Extended.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtendedRequestInformation {

	private static final String DEFAULT_PARAMETERS = "<no-query-string>";

	private static final String DEFAULT_PROTOCOL = "HTTP/1.1";

	private static final String DEFAULT_ENCODING = "<no-encoding>";

	private String uri;

	private int port;

	private String host;

	private String protocol;

	private String method;

	private String parameters;

	private String encoding;

	private int responseCode;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getProtocol() {
		return protocol == null ? DEFAULT_PROTOCOL : protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getParameters() {
		return parameters == null ? DEFAULT_PARAMETERS : parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getEncoding() {
		return encoding == null ? DEFAULT_ENCODING : encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

}
