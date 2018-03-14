package org.continuity.session.logs.converter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 * Class for converting invocation sequences into session logs.
 *
 * @author Alper Hidiroglu, Jonas Kunz, Tobias Angerstein, Henning Schulz
 *
 */
public class SessionConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionConverter.class);

	/**
	 * Converts the specified invocation sequences to session logs, naming it as specified in the
	 * business transactions.
	 *
	 * @param invocationSequences
	 *            The invocation sequences to be processed.
	 * @param businessTransactions
	 *            The business transactions consisting of the business transaction name and the
	 *            abstract URI (e.g., <code>/foo/{bar}/get</code>).
	 * @return The session logs as string.
	 */
	public String convertIntoSessionLog(Iterable<InvocationSequenceData> invocationSequences, HashMap<Long, Pair<String, String>> businessTransactions) {
		HashMap<String, List<HttpTimerData>> sortedList = sortBySessionAndTimestamp(invocationSequences);
		return getSessionLogsAsString(sortedList, businessTransactions);
	}

	/**
	 * @param sortedList
	 * @param methods
	 */
	public String getSessionLogsAsString(HashMap<String, List<HttpTimerData>> sortedList, HashMap<Long, Pair<String, String>> businessTransactions) {
		boolean first = true;
		String sessionLogs = "";
		for (Entry<String, List<HttpTimerData>> currentEntry : sortedList.entrySet()) {

			List<HttpTimerData> currentList = currentEntry.getValue();
			String sessionId = currentEntry.getKey();
			StringBuffer entry = new StringBuffer();
			entry.append(sessionId);

			boolean empty = true;
			boolean lastIsRedirect = false;

			for (HttpTimerData invoc : currentList) {
				Pair<String, String> bt = businessTransactions.get(invoc.getId());

				if ((bt != null)) {
					if (!lastIsRedirect) {
						entry.append(";\"").append(bt.getLeft()).append("\":").append(invoc.getTimeStamp().getTime() * 1000000).append(":")
								.append((invoc.getTimeStamp().getTime() * 1000000) + ((long) invoc.getDuration() * 1000000));

						appendHTTPInfo(entry, invoc, bt.getRight());
						empty = false;
					}

					lastIsRedirect = (invoc.getHttpResponseStatus() / 100) == 3;
				}
			}

			if (!empty) {
				if (first) {
					first = false;
				} else {
					sessionLogs += "\n";
				}
				sessionLogs += entry.toString();
			}
		}
		return sessionLogs;
	}

	/**
	 * @param invocationSequences
	 * @return
	 */
	public HashMap<String, List<HttpTimerData>> sortBySessionAndTimestamp(Iterable<InvocationSequenceData> invocationSequences) {

		HashMap<String, List<HttpTimerData>> sortedSessionsInvoc = new HashMap<String, List<HttpTimerData>>();

		ArrayList<HttpTimerData> sortedList = new ArrayList<HttpTimerData>();

		// Only InvocationSequenceData with SessionID != null
		for (InvocationSequenceData invoc : invocationSequences) {
			if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
				HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
				sortedList.add(dat);
			}
		}

		Collections.sort(sortedList, new Comparator<HttpTimerData>() {
			@Override
			public int compare(HttpTimerData data1, HttpTimerData data2) {
				int startTimeComparison = Long.compare(data1.getTimeStamp().getTime(), data2.getTimeStamp().getTime());

				if (startTimeComparison != 0) {
					return startTimeComparison;
				} else {
					return Double.compare(data1.getDuration(), data2.getDuration());
				}
			}
		});

		String firstSessionId = null;
		int firstSessionIdIndex = 0;

		for (HttpTimerData invoc : sortedList) {
			firstSessionId = extractSessionIdFromCookies(invoc);

			if (firstSessionId != null) {
				break;
			}

			firstSessionIdIndex++;
		}

		int i = 0;

		for (HttpTimerData invoc : sortedList) {
			String sessionId;

			if (i < firstSessionIdIndex) {
				sessionId = firstSessionId;
			} else {
				sessionId = extractSessionIdFromCookies(invoc);
			}

			if (sessionId != null) {
				if (sortedSessionsInvoc.containsKey(sessionId)) {
					sortedSessionsInvoc.get(sessionId).add(invoc);
				} else {
					List<HttpTimerData> newList = new ArrayList<HttpTimerData>();
					newList.add(invoc);
					sortedSessionsInvoc.put(sessionId, newList);
				}
			}

			i++;
		}
		return sortedSessionsInvoc;
	}

	/**
	 * @param entry
	 * @param seq
	 */
	private void appendHTTPInfo(StringBuffer entry, HttpTimerData dat, String abstractUri) {
		// TODO: Do we really need this huge try-catch block? (HSH)
		try {
			String uri = dat.getHttpInfo().getUri();

			if (uri == null) {
				LOGGER.error("Could not append HTTP info to {}: The URI was null!", entry);
				return;
			}

			String queryString = "<no-query-string>";
			Map<String, String[]> params = Optional.ofNullable(dat.getParameters()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());

			// TODO: This is a workaround because the session logs do not support parameters without
			// values (e.g., host/login?logout) and Wessbas fails if it is transformed to
			// host/login=logout=
			params = params.entrySet().stream().filter(e -> (e.getValue() != null) && (e.getValue().length > 0) && !"".equals(e.getValue()[0]))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

			params.putAll(extractUriParams(uri, abstractUri));

			if (params != null) {
				queryString = encodeQueryString(params);
			}

			String host_port = dat.getHeaders().get("host");
			String host = host_port;
			String port = "";
			if (host_port.contains(":")) {
				int i = host_port.indexOf(":");
				host = host_port.substring(0, i);
				port = host_port.substring(i + 1);
			}

			String protocol = "HTTP/1.1";
			String encoding = "<no-encoding>";
			String method = dat.getHttpInfo().getRequestMethod().toUpperCase();

			entry.append(":").append(abstractUri);
			entry.append(":").append(port);
			entry.append(":").append(host);
			entry.append(":").append(protocol);
			entry.append(":").append(method);
			entry.append(":").append(queryString);
			entry.append(":").append(encoding);
		} catch (NoSuchElementException e) {
			LOGGER.error("Some data is missing for appending the HTTP info, therefore omit writing. Result is {}.", entry);
		}

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
	private Map<String, String[]> extractUriParams(String uri, String urlPattern) {
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
	 * @param dat
	 * @return
	 */
	private String extractSessionIdFromCookies(HttpTimerData dat) {
		String sessionID = null;
		String cookies = dat.getHeaders().get("cookie");
		if (cookies != null) {
			int begin = cookies.indexOf("JSESSIONID=");
			// Vorher wurde abgefragt ob != -1 (warum?)
			if (begin != -1) {
				begin += "JSESSIONID=".length();
				sessionID = "";
				while (begin < cookies.length()) {
					char c = cookies.charAt(begin);
					if (!Character.isLetterOrDigit(c)) {
						break;
					} else {
						sessionID += c;
						begin++;
					}
				}
			}
		}
		return sessionID;
	}

	/**
	 * @param params
	 * @return
	 */
	private String encodeQueryString(Map<String, String[]> params) {
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
}
