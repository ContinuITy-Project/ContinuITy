package org.continuity.session.logs.extractor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.spec.research.open.xtrace.api.core.callables.HTTPRequestProcessing;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;

import open.xtrace.OPENxtraceUtils;

/**
 * Wraps {@link HTTPRequestProcessingImpl}
 *
 * @author tan
 *
 */
public class OPENxtraceHttpRequestData implements HTTPRequestData {
	/**
	 * Wrapped callable
	 */
	private HTTPRequestProcessing callable;

	public OPENxtraceHttpRequestData(HTTPRequestProcessing httpRequestProcessingCallable) {
		this.callable = httpRequestProcessingCallable;
	}

	@Override
	public String getIdentifier() {
		return Objects.toString(callable.getIdentifier().get());
	}

	@Override
	public long getTimestamp() {
		return callable.getTimestamp() * 1000000;
	}

	@Override
	public long getResponseTime() {
		return callable.getResponseTime();
	}

	@Override
	public String getUri() {
		return callable.getUri();
	}

	@Override
	public Optional<Map<String, String[]>> getHTTPParameters() {
		return callable.getHTTPParameters();
	}

	@Override
	public Optional<String> getRequestBody() {
		return callable.getRequestBody();
	}

	@Override
	public int getPort() {
		return callable.getContainingSubTrace().getLocation().getPort();
	}

	@Override
	public String getHost() {
		return callable.getContainingSubTrace().getLocation().getHost();
	}

	@Override
	public String getRequestMethod() {
		return callable.getRequestMethod().get().name();
	}

	@Override
	public long getResponseCode() {
		return callable.getResponseCode().get();
	}

	@Override
	public String getSessionId() {
		if (callable.getHTTPHeaders().isPresent() && callable.getHTTPHeaders().get().containsKey("cookie")) {
			return OPENxtraceUtils.extractSessionIdFromCookies(callable.getHTTPHeaders().get().get("cookie"));
		} else {
			return null;
		}
	}

	@Override
	public String getBusinessTransaction() {
		if ((callable.getContainingSubTrace() != null) && (callable.getContainingSubTrace().getLocation() != null)) {
			return callable.getContainingSubTrace().getLocation().getBusinessTransaction().orElse(null);
		} else {
			return null;
		}
	}
}
