package org.continuity.session.logs.entities;

import com.univocity.parsers.annotations.Parsed;

public class RowObject {
	
	@Parsed(field = "session-id")
	private String sessionID;

	@Parsed(field = "user-name")
	private String userName;
	
	@Parsed(field = "business-transaction")
	private String businessTransaction;
	
	@Parsed(field = "request-start-time")
	private String requestStartTime;
	
	@Parsed(field = "request-end-time")
	private String requestEndTime;
	
	@Parsed(field = "request-url")
	private String requestURL;
	
	@Parsed(field = "port")
	private String port;
	
	@Parsed(field = "host-ip")
	private String hostIP;
	
	@Parsed(field = "protocol")
	private String protocol;
	
	@Parsed(field = "method")
	private String method;
	
	@Parsed(field = "parameter")
	private String parameter;
	
	@Parsed(field = "encoding")
	private String encoding;

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getBusinessTransaction() {
		return businessTransaction;
	}

	public void setBusinessTransaction(String businessTransaction) {
		this.businessTransaction = businessTransaction;
	}

	public String getRequestStartTime() {
		return requestStartTime;
	}

	public void setRequestStartTime(String requestStartTime) {
		this.requestStartTime = requestStartTime;
	}

	public String getRequestEndTime() {
		return requestEndTime;
	}

	public void setRequestEndTime(String requestEndTime) {
		this.requestEndTime = requestEndTime;
	}

	public String getRequestURL() {
		return requestURL;
	}

	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getHostIP() {
		return hostIP;
	}

	public void setHostIP(String hostIP) {
		this.hostIP = hostIP;
	}

	public String getProtocol() {
		return protocol;
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

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	public String toString() {
		return "RowObject [sessionID=" + sessionID + ", userName=" + userName + ", businessTransaction="
				+ businessTransaction + ", requestStartTime=" + requestStartTime + ", requestEndTime=" + requestEndTime
				+ ", requestURL=" + requestURL + ", port=" + port + ", hostIP=" + hostIP + ", protocol=" + protocol
				+ ", method=" + method + ", parameter=" + parameter + ", encoding=" + encoding + "]";
	}
}
