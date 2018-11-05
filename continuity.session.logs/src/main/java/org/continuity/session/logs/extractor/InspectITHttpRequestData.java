package org.continuity.session.logs.extractor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import rocks.inspectit.shared.all.communication.data.HttpTimerData;

/**
 * Wraps {@link HttpTimerData}
 * 
 * @author Tobias Angerstein
 *
 */
public class InspectITHttpRequestData implements HTTPRequestData {
	/**
	 * Wrapped httpTimerData
	 */
	private HttpTimerData httpTimerData;

	/**
	 * Extracted host
	 */
	private String host;

	/**
	 * Extracted port
	 */
	private int port;

	public InspectITHttpRequestData(HttpTimerData httpTimerData) {
		this.httpTimerData = httpTimerData;
		String host_port = httpTimerData.getHeaders().get("host");
		if (host_port.contains(":")) {
			int i = host_port.indexOf(":");
			host = host_port.substring(0, i);
			port = Integer.parseInt(host_port.substring(i + 1));
		}
	}

	@Override
	public long getIdentifier() {
		return httpTimerData.getId();
	}

	@Override
	public long getTimestamp() {
		return httpTimerData.getTimeStamp().getTime()*1000000;
	}

	@Override
	public long getResponseTime() {
		return (long) httpTimerData.getDuration()*1000000;
	}

	@Override
	public String getUri() {
		return httpTimerData.getHttpInfo().getUri();
	}

	@Override
	public Optional<Map<String, String[]>> getHTTPParameters() {
		HashMap<String, String[]> parameters = new HashMap<String, String[]>();
		if (null != httpTimerData.getParameters()) {
			parameters.putAll(httpTimerData.getParameters());
		}
		if (null != httpTimerData.getHttpInfo().getQueryString()) {
			parameters.putAll(convertToParameterMap(httpTimerData.getHttpInfo().getQueryString()));
		}
		if (!parameters.isEmpty()) {
			return Optional.ofNullable(parameters);
		} else {
			return Optional.empty();
		}
	}

	@Override
	// Currently not explicitly provided by inspectIT
	public Optional<String> getRequestBody() {
		return Optional.empty();
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getRequestMethod() {
		return httpTimerData.getHttpInfo().getRequestMethod().toUpperCase();
	}

	@Override
	public long getResponseCode() {
		return httpTimerData.getHttpResponseStatus();
	}

	private Map<String, String[]> convertToParameterMap(String queryString) {
		HashMap<String, List<String>> parameters = new HashMap<String, List<String>>();
		StringTokenizer tokenizer = new StringTokenizer(queryString, "&");
		while (tokenizer.hasMoreTokens()) {
			String[] keyValuePair = tokenizer.nextToken().split("=");
			String key = keyValuePair[0];
			String value = keyValuePair[1];
			if (parameters.containsKey(key)) {
				parameters.get(key).add(value);
			} else {
				parameters.put(key, new ArrayList<String>(Arrays.asList(value)));
			}
		}
		return parameters.entrySet().stream().map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toArray(new String[e.getValue().size()])))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

}
