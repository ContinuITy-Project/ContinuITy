package org.continuity.session.logs.extractor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Abstract class converting arbitrary data
 *
 * @author Tobias Angerstein
 *
 * @param <T>
 *            data type of the input data
 */
public abstract class AbstractSessionLogsExtractor<T> {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSessionLogsExtractor.class);

	/**
	 * Tag of the application
	 */
	protected String tag;

	/**
	 * Eureka rest template
	 */
	protected final RestTemplate restTemplate;

	/**
	 * Constructor
	 *
	 * @param tag
	 *            tag of application
	 * @param eurekaRestTemplate
	 *            rest template
	 */
	protected AbstractSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate) {
		this.tag = tag;
		this.restTemplate = eurekaRestTemplate;
	}

	/**
	 * Extracts session logs from data.
	 *
	 * @param data
	 * @return
	 */
	public abstract String getSessionLogs(Iterable<T> data);

	/**
	 * Builds session logs
	 *
	 * @param sortedList
	 *            Map of requests per session log
	 * @param businessTransactions
	 *            Map of httpCallable identifier and pair of business transaction name and URI
	 * @return
	 */
	public String getSessionLogsAsString(HashMap<String, List<HTTPRequestData>> sortedList, HashMap<Long, Pair<String, String>> businessTransactions) {

		boolean first = true;
		StringBuffer sessionLogs = new StringBuffer();

		for (Entry<String, List<HTTPRequestData>> currentEntry : sortedList.entrySet()) {
			List<HTTPRequestData> requestList = currentEntry.getValue();
			String sessionId = currentEntry.getKey();
			StringBuffer entry = new StringBuffer();
			entry.append(sessionId);

			boolean empty = true;
			boolean lastIsRedirect = false;

			for (HTTPRequestData httpRequest : requestList) {
				Pair<String, String> bt = businessTransactions.get(httpRequest.getIdentifier());

				if ((bt != null)) {
					if (!lastIsRedirect) {
						entry.append(";\"").append(bt.getLeft()).append("\":").append(httpRequest.getTimestamp()).append(":")
								.append(httpRequest.getTimestamp() + (httpRequest.getResponseTime()));

						appendHTTPInfo(entry, httpRequest, bt.getRight());
						empty = false;
					}

					lastIsRedirect = (httpRequest.getResponseCode() / 100) == 3;
				}
			}

			if (!empty) {
				if (first) {
					first = false;
				} else {
					sessionLogs.append("\n");
				}
				sessionLogs.append(entry.toString());
			}
		}
		return sessionLogs.toString();
	}

	/**
	 * Appends the HTTP info of the {@link HTTPRequestProcessingImpl} to the session log
	 *
	 * @param entry
	 *            The session log
	 * @param httpRequest
	 *            {@link HTTPRequestProcessingImpl}
	 * @param abstractUri
	 *            abstract uri
	 */
	public void appendHTTPInfo(StringBuffer entry, HTTPRequestData httpRequest, String abstractUri) {
		String uri = httpRequest.getUri();

		if (uri == null) {
			LOGGER.error("Could not append HTTP info to {}: The URI was null!", entry);
			return;
		}

		String queryString = "<no-query-string>";
		Map<String, String[]> params = new HashMap<String, String[]>();

		if (httpRequest.getHTTPParameters().isPresent()) {
			params = Optional.ofNullable(httpRequest.getHTTPParameters().get()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());
		}

		// TODO: This is a workaround because the session logs do not support parameters without
		// values (e.g., host/login?logout) and Wessbas fails if it is transformed to
		// host/login=logout=
		params = params.entrySet().stream().filter(e -> (e.getValue() != null) && (e.getValue().length > 0) && !"".equals(e.getValue()[0])).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		params.putAll(extractUriParams(uri, abstractUri));

		if (httpRequest.getRequestBody().isPresent() && !httpRequest.getRequestBody().get().isEmpty()) {
			params.put("_BODY", new String[] { httpRequest.getRequestBody().get() });
		}

		if (params != null) {
			queryString = encodeQueryString(params);
		}

		String host = httpRequest.getHost();
		String port = String.valueOf(httpRequest.getPort());

		String protocol = "HTTP/1.1";
		String encoding = "<no-encoding>";
		String method = httpRequest.getRequestMethod();

		entry.append(":").append(abstractUri);
		entry.append(":").append(port);
		entry.append(":").append(host);
		entry.append(":").append(protocol);
		entry.append(":").append(method);
		entry.append(":").append(queryString);
		entry.append(":").append(encoding);
	}

	/**
	 * Extracts the parameters from the URI. E.g., if the URI pattern is
	 * <code>/foo/{bar}/get/{id}</code> and the actual URI is <code>/foo/abc/get/42</code>, the
	 * extracted parameters will be <code>URL_PART_bar=abc</code> and <code>URL_PARTid=42</code>.
	 *
	 * @param uri
	 *            The URI to extract the parameters from.
	 * @param urlPattern
	 *            The abstract URI that specifies the pattern.
	 * @return The extracted parameters in the form <code>[URL_PART_name -> value]</code>.
	 */
	protected Map<String, String[]> extractUriParams(String uri, String urlPattern) {
		String[] uriParts = normalizeUri(uri).split("\\/");
		String[] patternParts = normalizeUri(urlPattern).split("\\/");

		if (uriParts.length != patternParts.length) {
			throw new IllegalArgumentException("Uri and pattern need to have the same length, bus was '" + uri + "' and '" + urlPattern + "'!");
		}

		Map<String, String[]> params = new HashMap<>();

		for (int i = 0; i < uriParts.length; i++) {
			if (patternParts[i].matches("\\{.*\\}")) {
				String param = patternParts[i].substring(1, patternParts[i].length() - 1);
				params.put("URL_PART_" + param, new String[] { uriParts[i] });
			}
		}

		return params;
	}

	private String normalizeUri(String uri) {
		if (!uri.startsWith("/")) {
			uri = "/" + uri;
		}

		if (!uri.endsWith("/")) {
			uri = uri + "/";
		}

		return uri;
	}

	/**
	 * Extracts the session id from cookies
	 *
	 * @param cookies
	 *            the cookies of the request
	 * @return the session id
	 */
	protected String extractSessionIdFromCookies(String cookies) {
		String sessionID = null;
		if (cookies != null) {
			int begin = -1;

			if (cookies.indexOf("JSESSIONID=") != -1) {
				begin = cookies.indexOf("JSESSIONID=") + "JSESSIONID=".length();
			} else if (cookies.indexOf("md.sid=") != -1) {
				begin = cookies.indexOf("md.sid=") + "md.sid=".length();
			} else {
				return null;
			}

			sessionID = "";

			while (begin < cookies.length()) {
				char c = cookies.charAt(begin);
				if (c == ';') {
					break;
				}
				sessionID += c;
				begin++;
			}
		}

		return sessionID;
	}

	/**
	 * Encodes a map of parameters into a query string
	 *
	 * @param params
	 * @return
	 */
	protected String encodeQueryString(Map<String, String[]> params) {
		try {
			if (params.isEmpty()) {
				return "<no-query-string>";
			}
			StringBuffer result = new StringBuffer();
			for (String key : params.keySet()) {
				String encodedKey = URLEncoder.encode(key, "UTF-8");
				for (String value : params.get(key)) {
					String encodedValue = "";
					if (value != null) {
						encodedValue = "=" + URLEncoder.encode(value, "UTF-8");
					}

					if (result.length() > 0) {
						result.append("&");
					}
					result.append(encodedKey + encodedValue);

				}
			}
			return result.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	protected Application retrieveApplicationModel(String tag) {
		if (tag == null) {
			LOGGER.warn("Cannot retrieve the application model for naming the Session Logs. The tag is nulL!");
			return null;
		}

		try {
			return restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Received error status code when asking for system model with tag " + tag, e);
			return null;
		}
	}
}
