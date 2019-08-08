package org.continuity.commons.idpa;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;

/**
 * Extracts the parameters from the URI. E.g., if the URI pattern is
 * <code>/foo/{bar}/get/{id}</code> and the actual URI is <code>/foo/abc/get/42</code>, the
 * extracted parameters will be <code>bar=abc</code> and <code>id=42</code>.
 *
 * @author Henning Schulz
 *
 */
public class UrlPartParameterExtractor {

	private final Matcher matcher;

	private final boolean matches;

	private final Iterator<String> parameters;

	private String currParam;

	public UrlPartParameterExtractor(HttpEndpoint endpoint, String path) {
		this.matcher = Pattern.compile(endpoint.getPathAsRegex()).matcher(path);
		this.matches = matcher.find();
		this.parameters = endpoint.getParameters().stream().filter(p -> p.getParameterType() == HttpParameterType.URL_PART).map(HttpParameter::getName).iterator();
	}

	/**
	 * Returns whether there is a next parameter value pair to be extracted.
	 *
	 * @return {@code true} if there is a next pair.
	 */
	public boolean hasNext() {
		return parameters.hasNext();
	}

	/**
	 * Returns the next parameter name. Calling it two times in a row will result in two subsequent
	 * parameter names, e.g. {@code a} and {@code b} in <code>/foo/{a}/bar{b}</code>.
	 *
	 * @return The parameter name or {@code null} if there is none.
	 */
	public String nextParameter() {
		currParam = parameters.next();
		return currParam;
	}

	/**
	 * Returns the value corresponding to the last retrieved parameter name. Requires
	 * {@link #nextParameter()} to be called first.
	 *
	 * @return The parameter value or {@code null} if there is none.
	 */
	public String currentValue() {
		if (!matches || (currParam == null)) {
			return null;
		} else {
			try {
				return matcher.group(currParam);
			} catch (IllegalArgumentException e) {
				// There is no such group
				return null;
			}
		}
	}

}
