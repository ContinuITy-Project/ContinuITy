package org.continuity.commons.utils;

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

}
