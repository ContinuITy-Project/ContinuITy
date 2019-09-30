package org.continuity.commons.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Henning Schulz
 *
 */
public class WebUtils {

	private WebUtils() {
		// Should not be instantiated
	}

	public static String addProtocolIfMissing(String url) {
		if (url == null) {
			return null;
		}

		if (url.startsWith("http")) {
			return url;
		} else {
			return "http://" + url;
		}
	}

	public static String buildUrl(String host, String port) {
		return buildUrl(host, port, "", false);
	}

	public static String buildUrl(String host, String port, boolean https) {
		return buildUrl(host, port, "", https);
	}

	public static String buildUrl(String host, String port, String path) {
		return buildUrl(host, port, path, false);
	}

	public static String buildUrl(String host, String port, String path, boolean https) {
		StringBuilder builder = new StringBuilder();

		if (https) {
			builder.append("https://");
		} else {
			builder.append("http://");
		}

		builder.append(host);
		builder.append(":");
		builder.append(port);
		builder.append(path);

		return builder.toString();
	}

	/**
	 * Formats the query parameters as a map.
	 *
	 * @param params
	 *            The query parameters of the following format: {@code param1=val1&param2=val2&...}.
	 *            Can be empty or {@code null}.
	 * @return
	 */
	public static Map<String, String[]> formatQueryParameters(String params) {
		if ((params == null) || params.isEmpty()) {
			return Collections.emptyMap();
		} else {
			if (params.startsWith("\\?")) {
				params = params.substring(1);
			}

			return Arrays.stream(params.split("&")).map(p -> {
				String[] pv = p.split("=");
				return Pair.of(pv[0], pv.length > 1 ? new String[] { pv[1] } : new String[] {});
			}).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		}
	}

}
