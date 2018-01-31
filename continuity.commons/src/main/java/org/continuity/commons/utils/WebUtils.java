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

}
