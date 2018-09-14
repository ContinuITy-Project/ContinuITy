package org.continuity.commons.idpa;

import java.util.Objects;

import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

/**
 * Can be used to map URIs of requests to {@link HttpEndpoint}s of a {@link Application}.
 *
 * @author Henning Schulz
 *
 */
public class RequestUriMapper {

	private final Application application;

	public RequestUriMapper(Application application) {
		this.application = application;
	}

	/**
	 * Maps the specified URI to an {@link HttpEndpoint} that has exactly the same URI. Wildcards
	 * (<code>{some-name}</code>) are treated as any other element of the URI. Hence, if you pass
	 * <code>/a/uri/with/{id}</code>, an interface with <code>/a/uri/with/{ident}</code> will
	 * <b>not</b> match.
	 *
	 * @param uri
	 *            The URI to be mapped.
	 * @param method
	 *            The request method.
	 * @return An {@link HttpEndpoint} that has exactly the same URI or {@code null} if there is no
	 *         such interface.
	 */
	public HttpEndpoint mapExactly(String uri, String method) {
		MappingFinder finder = new MappingFinder(uri, method);
		IdpaByClassSearcher<HttpEndpoint> searcher = new IdpaByClassSearcher<>(HttpEndpoint.class, finder::testExactly);
		searcher.visit(application);

		return finder.getFound();
	}

	/**
	 * Maps the specified URI to an {@link HttpEndpoint} that has the same URI, respecting
	 * wildcards. That is, if you pass <code>/a/uri/with/12345</code>, <code>/a/uri/with/{id}</code>
	 * will match.
	 *
	 * @param uri
	 *            The URI to be mapped.
	 * @param method
	 *            The request method.
	 * @return An {@link HttpEndpoint} with the same URI or {@code null} if there is no such
	 *         interface.
	 */
	public HttpEndpoint mapRespectingWildcards(String uri, String method) {
		MappingFinder finder = new MappingFinder(uri, method);
		IdpaByClassSearcher<HttpEndpoint> searcher = new IdpaByClassSearcher<>(HttpEndpoint.class, finder::testRespectingWildcards);
		searcher.visit(application);

		return finder.getFound();
	}

	/**
	 * Maps the specified URI to an {@link HttpEndpoint} that has the same URI. First, the URI is
	 * tested for exact similarity (by calling {@link #mapExactly(String)} and then, if there is not
	 * exact match, wildcards are respected (by calling {@link #mapRespectingWildcards(String)}.
	 *
	 * @param uri
	 *            The URI to be mapped.
	 * @param method
	 *            The request method.
	 * @return An {@link HttpEndpoint} with the same URI or {@code null} if there is no such
	 *         interface.
	 */
	public HttpEndpoint map(String uri, String method) {
		HttpEndpoint exactlyMapped = mapExactly(uri, method);

		if (exactlyMapped != null) {
			return exactlyMapped;
		} else {
			return mapRespectingWildcards(uri, method);
		}
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

	private class MappingFinder {

		private final String uri;
		private final String[] uriParts;

		private final String method;

		private HttpEndpoint found = null;

		public MappingFinder(String uri, String method) {
			this.uri = normalizeUri(uri);
			this.uriParts = this.uri.split("\\/");
			this.method = method;
		}

		public HttpEndpoint getFound() {
			return found;
		}

		public void testExactly(HttpEndpoint interf) {
			if ((found == null) && Objects.equals(method, interf.getMethod()) && Objects.equals(uri, normalizeUri(interf.getPath()))) {
				found = interf;
			}
		}

		public void testRespectingWildcards(HttpEndpoint interf) {
			String[] interfUriParts = normalizeUri(interf.getPath()).split("\\/");

			if ((found != null) || !method.equals(interf.getMethod()) || (uriParts.length != interfUriParts.length)) {
				return;
			} else {
				for (int i = 0; i < uriParts.length; i++) {
					if (!uriPartsMap(uriParts[i], interfUriParts[i])) {
						return;
					}
				}

				found = interf;
			}
		}

		private boolean uriPartsMap(String uri, String pattern) {
			return isWildcard(pattern) || uri.equals(pattern);
		}

		private boolean isWildcard(String uriPart) {
			return uriPart.matches("\\{.*\\}");
		}

	}

}
