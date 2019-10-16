package org.continuity.cobra.converter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.continuity.commons.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.HTTPMethod;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.SubTraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.TraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;

import open.xtrace.OPENxtraceUtils;

/**
 * Converts session logs in the extended format to OPEN.xtrace.
 *
 * @author Henning Schulz
 *
 */
public class SessionLogsToOpenXtraceConverter implements OpenXtraceConverter<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsToOpenXtraceConverter.class);

	private final AtomicLong idCounter = new AtomicLong(0);

	@Override
	public List<Trace> convert(List<String> sessionLogs) {
		return sessionLogs.stream().flatMap(this::convert).collect(Collectors.toList());
	}

	private Stream<Trace> convert(String session) {
		String[] elements = session.split(";");

		if (elements.length < 2) {
			LOGGER.warn("Found empty session. Ignoring.");
			return Stream.empty();
		}

		String sessionId = elements[0];

		return Arrays.stream(elements).skip(1).map(s -> convert(sessionId, s));
	}

	/**
	 *
	 *
	 * @param sessionId
	 * @param request
	 *            Request of the following format:
	 *            {@code "name":startNanos:endNanos:path:port:host:protocol:method:parameters:encoding}
	 * @return
	 */
	private Trace convert(String sessionId, String request) {
		String[] elements = request.split(":");

		String name = elements[0].substring(1, elements[0].length() - 1);
		long startNanos = Long.parseLong(elements[1]);
		long endNanos = Long.parseLong(elements[2]);
		String path = elements[3];
		int port = Integer.parseInt(elements[4]);
		String host = elements[5];
		String method = elements[7];
		String parameters = elements[8];

		TraceImpl trace = new TraceImpl((request.hashCode() * 31) + idCounter.getAndIncrement());
		trace.setRoot(createSubTrace(trace, sessionId, name, startNanos, endNanos, path, port, host, method, parameters));
		return trace;
	}

	private SubTraceImpl createSubTrace(TraceImpl trace, String sessionId, String name, long startNanos, long endNanos, String path, int port, String host, String method, String parameters) {
		SubTraceImpl subTrace = new SubTraceImpl(trace.getTraceId(), null, trace);

		LocationImpl location = new LocationImpl(host, port, null, null, name);
		subTrace.setLocation(location);

		subTrace.setRoot(createCallable(subTrace, sessionId, startNanos, endNanos, path, method, parameters));

		return subTrace;
	}

	private HTTPRequestProcessingImpl createCallable(SubTraceImpl subTrace, String sessionId, long startNanos, long endNanos, String path, String method, String parameters) {
		HTTPRequestProcessingImpl request = new HTTPRequestProcessingImpl(null, subTrace);
		request.setIdentifier(Long.toHexString(subTrace.getSubTraceId()));

		request.setTimestamp(startNanos / MILLIS_TO_NANOS);
		request.setResponseTime(endNanos - startNanos);
		request.setUri(path);
		request.setRequestMethod(HTTPMethod.valueOf(method.toUpperCase()));
		request.setHTTPParameters(WebUtils.formatQueryParameters(parameters));

		OPENxtraceUtils.setSessionId(request, sessionId);

		return request;
	}

}
