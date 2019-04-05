package org.continuity.idpa.application.entities;

public class EndpointAsRegex {

	private String domain;

	private String port;

	private String method;

	private String regex;

	public EndpointAsRegex() {
	}

	public EndpointAsRegex(String method, String regex) {
		this.method = method;
		this.regex = regex;
	}

	public EndpointAsRegex(String domain, String port, String method, String regex) {
		this.domain = domain;
		this.port = port;
		this.method = method;
		this.regex = regex;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

}
