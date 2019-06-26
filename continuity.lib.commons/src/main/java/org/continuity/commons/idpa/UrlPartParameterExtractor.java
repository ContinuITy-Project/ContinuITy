package org.continuity.commons.idpa;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Extracts the parameters from the URI. E.g., if the URI pattern is
 * <code>/foo/{bar}/get/{id}</code> and the actual URI is <code>/foo/abc/get/42</code>, the
 * extracted parameters will be <code>bar=abc</code> and <code>id=42</code>.
 *
 * @author Henning Schulz
 *
 */
public class UrlPartParameterExtractor {

	private final String[] patternParts;

	private final String[] pathParts;

	private int currentIndex = -1;

	public UrlPartParameterExtractor(String pattern, String path) {
		this.pathParts = normalizeUri(path).split("\\/");
		this.patternParts = normalizeUri(pattern).split("\\/");
	}

	/**
	 * Returns whether there is a next parameter value pair to be extracted.
	 *
	 * @return {@code true} if there is a next pair.
	 */
	public boolean hasNext() {
		return nextParameterIndex() < patternParts.length;
	}

	/**
	 * Returns the next parameter name. Calling it two times in a row will result in two subsequent
	 * parameter names, e.g. {@code a} and {@code b} in <code>/foo/{a}/bar{b}</code>.
	 *
	 * @return The parameter name or {@code null} if there is none.
	 */
	public String nextParameter() {
		int nextIndex = nextParameterIndex();

		if (nextIndex < patternParts.length) {
			currentIndex = nextIndex;

			if (isTrailingWildcard(nextIndex)) {
				return patternParts[nextIndex].substring(1, patternParts[nextIndex].length() - 3);
			} else {
				return patternParts[nextIndex].substring(1, patternParts[nextIndex].length() - 1);
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the value corresponding to the last retrieved parameter name. Requires
	 * {@link #nextParameter()} to be called first.
	 *
	 * @return The parameter value or {@code null} if there is none.
	 */
	public String currentValue() {
		if (currentIndex < pathParts.length) {
			if (isTrailingWildcard(currentIndex)) {
				return Arrays.stream(pathParts).skip(currentIndex).collect(Collectors.joining("/"));
			} else {
				return pathParts[currentIndex];
			}
		} else {
			return null;
		}
	}

	private int nextParameterIndex() {
		for (int i = currentIndex + 1; i < patternParts.length; i++) {
			if (patternParts[i].matches("\\{.*\\}")) {
				return i;
			}
		}

		return Integer.MAX_VALUE;
	}

	private boolean isTrailingWildcard(int index) {
		return patternParts[index].matches("\\{.*\\:\\*\\}");
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

}
