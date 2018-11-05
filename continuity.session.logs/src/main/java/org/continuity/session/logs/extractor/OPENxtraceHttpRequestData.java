package org.continuity.session.logs.extractor;

import java.util.Map;
import java.util.Optional;

import org.spec.research.open.xtrace.api.core.callables.HTTPRequestProcessing;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;

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
	public long getIdentifier() {
		return (Long) callable.getIdentifier().get();
	}

	@Override
	public long getTimestamp() {
		return callable.getTimestamp()*1000000;
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
}
