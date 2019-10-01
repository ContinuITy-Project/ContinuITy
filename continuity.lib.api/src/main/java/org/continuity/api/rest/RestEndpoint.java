package org.continuity.api.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Represents one REST endpoint of a service, containing the service name, the path incl. path
 * parameters and the request method.
 *
 * @author Henning Schulz
 *
 */
public class RestEndpoint {

	private final String serviceName;

	private final String root;

	private final List<StringOrPar> elements;

	private final RequestMethod method;

	private RestEndpoint(String serviceName, String root, List<StringOrPar> elements, RequestMethod method) {
		this.serviceName = serviceName;
		this.root = root == null ? "" : root;
		this.elements = elements;
		this.method = method;
	}

	/**
	 * Creates a new instance.
	 *
	 * @param serviceName
	 *            The name of the service.
	 * @param root
	 *            The root path element.
	 * @param path
	 *            The path (without the root).
	 * @param method
	 *            The request method.
	 * @return The new RestEndpoint instance.
	 */
	protected static RestEndpoint of(String serviceName, String root, String path, RequestMethod method) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		String[] pathElements = path.split("\\/");
		List<StringOrPar> elements = new ArrayList<>();

		for (String elem : pathElements) {
			if (elem.startsWith("{")) {
				elements.add(StringOrPar.of(PathPar.of(elem.substring(1, elem.length() - 1))));
			} else {
				elements.add(StringOrPar.of(elem));
			}
		}

		return new RestEndpoint(serviceName, root, elements, method);
	}

	/**
	 * Redirects a URL targeting a backend service via the orchestrator.
	 *
	 * @param url
	 *            The URL targeting the backend service.
	 * @param orchestratorHost
	 *            The host of the orchestrator service.
	 * @return A URL targeting the backend service via the orchestrator.
	 */
	public static String urlViaOrchestrator(String url, String orchestratorHost) {
		String protocol = "http://";

		if (url.startsWith("http://")) {
			url = url.substring(7);
		} else if (url.startsWith("https://")) {
			protocol = "https://";
			url = url.substring(8);
		}

		return new StringBuilder().append(protocol).append(orchestratorHost).append("/").append(url).toString();
	}

	/**
	 * Redirects a URL targeting a backend service via the orchestrator, using
	 * {@link RestApi.Orchestrator#SERVICE_NAME}.
	 *
	 * @param url
	 *            The URL targeting the backend service.
	 * @return A URL targeting the backend service via the orchestrator.
	 */
	public static String urlViaOrchestrator(String url) {
		return urlViaOrchestrator(url, RestApi.Orchestrator.SERVICE_NAME);
	}

	/**
	 * Redirects this endpoint via the orchestrator service.
	 *
	 * @return An endpoint indirectly targeting the original endpoint via the orchestrator.
	 */
	public RestEndpoint viaOrchestrator() {
		return of(RestApi.Orchestrator.SERVICE_NAME, "/" + this.serviceName, genericPath(), this.method);
	}

	/**
	 * Returns whether this endpoint has a root element.
	 *
	 * @return {@code true} if it has a root element.
	 */
	public boolean hasRoot() {
		return !"".equals(root);
	}

	/**
	 * Returns the represented generic path, e.g., <code>/my/path/{id}</code> where
	 * <code>{id}</code> denotes a parameter.
	 *
	 * @return The generic path as string.
	 */
	public String genericPath() {
		return elements.stream().reduce(StringOrPar.of(root), StringOrPar::concat).toString();
	}

	/**
	 * Returns the represented path with parameter instance values, e.g., <code>/my/path/MyId</code>
	 * for the generic path <code>/my/path/{id}</code>.
	 *
	 * @param values
	 *            The parameter values to fill in the parameters.
	 * @return The path as string.
	 */
	public String path(Object... values) {
		List<Object> valueList = Arrays.asList(values);
		Collections.reverse(valueList);

		Stack<Object> valueStack = new Stack<>();
		valueStack.addAll(valueList);

		return elements.stream().reduce(StringOrPar.of(root), (a, b) -> StringOrPar.concatWithValues(a, b, valueStack)).toString();
	}

	/**
	 * Creates the complete request URL.
	 *
	 * @param values
	 *            The values for the path parameters.
	 * @return A builder for getting and modifying the URL.
	 */
	public RequestBuilder requestUrl(Object... values) {
		return new RequestBuilder(serviceName, path(values));
	}

	/**
	 * Gets the request method.
	 *
	 * @return The request method of this endpoint.
	 */
	public RequestMethod method() {
		return method;
	}

	/**
	 * Checks if the link matches the endpoint and parses the path variables.
	 *
	 * @param link
	 *            The link to be parsed
	 * @return The path variables or {@code null} if the link does not match the endpoint.
	 */
	public List<String> parsePathParameters(String link) {
		String[] linkElements = normalizeLink(link).split("\\/");

		// Endpoint is serviceName/[root/]elements
		if (linkElements.length != (elements.size() + (hasRoot() ? 2 : 1))) {
			return null;
		}

		int i = 1;

		if (hasRoot()) {
			if (!root.equals(linkElements[1]) && !root.equals("/" + linkElements[1])) {
				return null;
			}

			i++;
		}

		List<String> params = new ArrayList<>();

		for (StringOrPar stringOrPar : elements) {
			if (stringOrPar.isPar()) {
				params.add(linkElements[i]);
			} else if (!stringOrPar.toString().equals(linkElements[i])) {
				return null;
			}

			i++;
		}

		return params;
	}

	private String normalizeLink(String link) {
		if (link.startsWith("http://")) {
			link = link.substring(7);
		} else if (link.startsWith("https://")) {
			link = link.substring(8);
		}

		if (link.startsWith("/")) {
			link = link.substring(1);
		}

		if (link.endsWith("/")) {
			link = link.substring(0, link.length() - 1);
		}

		return link;
	}

	/**
	 * Represents either a string or a {@link PathPar}. For usage in REST paths with parameters.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class StringOrPar {

		private final String string;
		private final PathPar par;

		private StringOrPar(String string, PathPar par) {
			this.string = string;
			this.par = par;
		}

		/**
		 * Creates an instance representing a string.
		 *
		 * @param string
		 *            The string value.
		 * @return The instance.
		 */
		public static StringOrPar of(String string) {
			return new StringOrPar(string, null);
		}

		/**
		 * Creates an instance representing a {@link PathPar}.
		 *
		 * @param par
		 *            The {@link PathPar} value.
		 * @return The instance.
		 */
		public static StringOrPar of(PathPar par) {
			return new StringOrPar(null, par);
		}

		/**
		 * Concats two instances.
		 *
		 * @param first
		 *            The first instance.
		 * @param second
		 *            The second instance.
		 * @return <code>first/second</code>.
		 */
		public static StringOrPar concat(StringOrPar first, StringOrPar second) {
			if ("/".equals(second.toString()) || "".equals(second.toString())) {
				return first;
			}

			return StringOrPar.of(first + "/" + second);
		}

		/**
		 * Note: Assumes that the first element is always string!
		 *
		 * @param first
		 * @param second
		 * @param values
		 * @return
		 */
		public static StringOrPar concatWithValues(StringOrPar first, StringOrPar second, Stack<Object> values) {
			if ("/".equals(second.toString()) || "".equals(second.toString())) {
				return first;
			}

			if (second.isPar()) {
				return StringOrPar.of(first + "/" + values.pop());
			} else {
				return StringOrPar.of(first + "/" + second);
			}
		}

		/**
		 * Returns whether this instance represents a string.
		 *
		 * @return {@code true}, if it is a string.
		 */
		public boolean isString() {
			return string != null;
		}

		/**
		 * Returns whether this instance represents a {@link PathPar}.
		 *
		 * @return {@code true}, if it is a {@link PathPar}.
		 */
		public boolean isPar() {
			return par != null;
		}

		@Override
		public String toString() {
			if (isPar()) {
				return par.generic();
			} else {
				return string;
			}
		}

	}

}
