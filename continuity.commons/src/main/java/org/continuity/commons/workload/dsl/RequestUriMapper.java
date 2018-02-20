package org.continuity.commons.workload.dsl;

import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;

/**
 * Can be used to map URIs of requests to {@link HttpInterface}s of a {@link SystemModel}.
 *
 * @author Henning Schulz
 *
 */
public class RequestUriMapper {

	private final SystemModel system;

	public RequestUriMapper(SystemModel system) {
		this.system = system;
	}

	/**
	 * Maps the specified URI to an {@link HttpInterface} that has exactly the same URI. Wildcards
	 * (<code>{some-name}</code>) are treated as any other element of the URI. Hence, if you pass
	 * <code>/a/uri/with/{id}</code>, an interface with <code>/a/uri/with/{ident}</code> will
	 * <b>not</b> match.
	 *
	 * @param uri
	 *            The URI to be mapped.
	 * @param method
	 *            The request method.
	 * @return An {@link HttpInterface} that has exactly the same URI or {@code null} if there is no
	 *         such interface.
	 */
	public HttpInterface mapExactly(String uri, String method) {
		MappingFinder finder = new MappingFinder(uri, method);
		ContinuityByClassSearcher<HttpInterface> searcher = new ContinuityByClassSearcher<>(HttpInterface.class, finder::testExactly);
		searcher.visit(system);

		return finder.getFound();
	}

	/**
	 * Maps the specified URI to an {@link HttpInterface} that has the same URI, respecting
	 * wildcards. That is, if you pass <code>/a/uri/with/12345</code>, <code>/a/uri/with/{id}</code>
	 * will match.
	 *
	 * @param uri
	 *            The URI to be mapped.
	 * @param method
	 *            The request method.
	 * @return An {@link HttpInterface} with the same URI or {@code null} if there is no such
	 *         interface.
	 */
	public HttpInterface mapRespectingWildcards(String uri, String method) {
		MappingFinder finder = new MappingFinder(uri, method);
		ContinuityByClassSearcher<HttpInterface> searcher = new ContinuityByClassSearcher<>(HttpInterface.class, finder::testRespectingWildcards);
		searcher.visit(system);

		return finder.getFound();
	}

	/**
	 * Maps the specified URI to an {@link HttpInterface} that has the same URI. First, the URI is
	 * tested for exact similarity (by calling {@link #mapExactly(String)} and then, if there is not
	 * exact match, wildcards are respected (by calling {@link #mapRespectingWildcards(String)}.
	 *
	 * @param uri
	 *            The URI to be mapped.
	 * @param method
	 *            The request method.
	 * @return An {@link HttpInterface} with the same URI or {@code null} if there is no such
	 *         interface.
	 */
	public HttpInterface map(String uri, String method) {
		HttpInterface exactlyMapped = mapExactly(uri, method);

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

		private HttpInterface found = null;

		public MappingFinder(String uri, String method) {
			this.uri = normalizeUri(uri);
			this.uriParts = this.uri.split("\\/");
			this.method = method;
		}

		public HttpInterface getFound() {
			return found;
		}

		public void testExactly(HttpInterface interf) {
			if ((found == null) && method.equals(interf.getMethod()) && uri.equals(normalizeUri(interf.getPath()))) {
				found = interf;
			}
		}

		public void testRespectingWildcards(HttpInterface interf) {
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
