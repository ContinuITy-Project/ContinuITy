package org.continuity.session.logs.converter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 * Class for converting invocation sequences into session logs.
 *
 * @author Alper Hidiroglu, Jonas Kunz, Tobias Angerstein
 *
 */
public class SessionConverter {

	/**
	 * @param methods
	 * @param agent
	 * @param invocationSequences
	 */
	public String convertIntoSessionLog(Iterable<InvocationSequenceData> invocationSequences, HashMap<Long, String> businessTransactions) {
		HashMap<String, List<HttpTimerData>> sortedList = sortAfterSessionAndTimestamp(invocationSequences);
		return getSessionLogsAsString(sortedList, businessTransactions);
	}

	/**
	 * @param sortedList
	 * @param methods
	 */
	public String getSessionLogsAsString(HashMap<String, List<HttpTimerData>> sortedList, HashMap<Long, String> businessTransactions) {
		boolean first = true;
		String sessionLogs = "";
		for (List<HttpTimerData> currentList : sortedList.values()) {

			HttpTimerData firstElement = currentList.get(0);
			String sessionId = extractSessionIdFromCookies(firstElement);
			StringBuffer entry = new StringBuffer();
			entry.append(sessionId);

			boolean empty = true;

			for (HttpTimerData invoc : currentList) {
				if (businessTransactions.get(invoc.getId()) != null) {
					entry.append(";\"").append(businessTransactions.get(invoc.getId())).append("\":").append(invoc.getTimeStamp().getTime() * 1000000).append(":")
					.append((invoc.getTimeStamp().getTime() * 1000000) + ((long) invoc.getDuration() * 1000000));

					appendHTTPInfo(entry, invoc);
					empty = false;
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
	public HashMap<String, List<HttpTimerData>> sortAfterSessionAndTimestamp(Iterable<InvocationSequenceData> invocationSequences) {

		HashMap<String, List<HttpTimerData>> sortedSessionsInvoc = new HashMap<String, List<HttpTimerData>>();

		ArrayList<HttpTimerData> sortedList = new ArrayList<HttpTimerData>();

		// Only InvocationSequenceData with SessionID != null
		for (InvocationSequenceData invoc : invocationSequences) {
			if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
				HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
				if (extractSessionIdFromCookies(dat) != null) {
					sortedList.add(dat);
				}
			}
		}

		Collections.sort(sortedList, new Comparator<HttpTimerData>() {
			@Override
			public int compare(HttpTimerData data1, HttpTimerData data2) {
				return data1.getTimeStamp().getTime() > data2.getTimeStamp().getTime() ? 1 : (data1.getTimeStamp().getTime() < data2.getTimeStamp().getTime()) ? -1 : 0;
			}
		});

		for (HttpTimerData invoc : sortedList) {
			String sessionId = extractSessionIdFromCookies(invoc);
			if (sortedSessionsInvoc.containsKey(sessionId)) {
				sortedSessionsInvoc.get(sessionId).add(invoc);
			} else {
				List<HttpTimerData> newList = new ArrayList<HttpTimerData>();
				newList.add(invoc);
				sortedSessionsInvoc.put(sessionId, newList);
			}
		}
		return sortedSessionsInvoc;
	}

	/**
	 * @param entry
	 * @param seq
	 */
	private void appendHTTPInfo(StringBuffer entry, HttpTimerData dat) {
		try {

			String uri = dat.getHttpInfo().getUri();
			if (uri == null) {
				return;
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
			String queryString = "<no-query-string>";
			Map<String, String[]> params = Optional.ofNullable(dat.getParameters()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());
			if (params != null) {
				queryString = encodeQueryString(params);
			}

			entry.append(":").append(uri);
			entry.append(":").append(port);
			entry.append(":").append(host);
			entry.append(":").append(protocol);
			entry.append(":").append(method);
			entry.append(":").append(queryString);
			entry.append(":").append(encoding);
		} catch (NoSuchElementException e) {
			return; // do nothing, some data is missing therefore omit writing
		}

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
