package org.continuity.cobra.converter;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.commons.accesslogs.ParameterRecord;
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
 * Converts {@link AccessLogEntry} to {@link Trace}.
 *
 * @author Henning Schulz
 *
 */
public class AccessLogsToOpenXtraceConverter implements OpenXtraceConverter<AccessLogEntry> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogsToOpenXtraceConverter.class);

	private final boolean hashSessionId;

	private final AtomicLong idCounter = new AtomicLong(0);

	public AccessLogsToOpenXtraceConverter(boolean hashSessionId) {
		this.hashSessionId = hashSessionId;
	}

	@Override
	public List<Trace> convert(List<AccessLogEntry> accessLogs) {
		return accessLogs.stream().filter(Objects::nonNull).map(this::convert).collect(Collectors.toList());
	}

	private Trace convert(AccessLogEntry entry) {
		TraceImpl trace = new TraceImpl((entry.hashCode() * 31) + idCounter.getAndIncrement());
		trace.setRoot(createSubTrace(trace, entry));
		return trace;
	}

	private SubTraceImpl createSubTrace(TraceImpl trace, AccessLogEntry entry) {
		SubTraceImpl subTrace = new SubTraceImpl(trace.getTraceId(), null, trace);

		LocationImpl location = new LocationImpl(null, 0, null, null, entry.getEndpoint());

		subTrace.setLocation(location);

		subTrace.setRoot(createCallable(subTrace, entry));

		return subTrace;
	}

	private HTTPRequestProcessingImpl createCallable(SubTraceImpl subTrace, AccessLogEntry entry) {
		HTTPRequestProcessingImpl request = new HTTPRequestProcessingImpl(null, subTrace);
		request.setIdentifier(Long.toHexString(subTrace.getSubTraceId()));

		try {
			request.setTimestamp(entry.getAccessTimeAsDate().getTime());
		} catch (ParseException e) {
			LOGGER.error("Could not parse timestamp!", e);
		}

		request.setUri(entry.getPath());
		request.setRequestMethod(HTTPMethod.valueOf(entry.getRequestMethod().toUpperCase()));
		request.setResponseCode(entry.getStatusCode());
		request.setResponseTime(entry.getResponseTime() * MICROS_TO_NANOS);
		request.setHTTPParameters(formatParameters(entry.getRequestParameters()));

		OPENxtraceUtils.setSessionId(request, hashSessionId ? DigestUtils.sha256Hex(entry.getClientHost()) : entry.getClientHost());

		return request;
	}

	private Map<String, String[]> formatParameters(List<ParameterRecord> params) {
		if ((params == null) || params.isEmpty()) {
			return Collections.emptyMap();
		} else {
			return params.stream().collect(Collectors.groupingBy(ParameterRecord::getName)).entrySet().stream().collect(Collectors.toMap(Entry::getKey, this::paramListToArray));
		}
	}

	private String[] paramListToArray(Entry<String, List<ParameterRecord>> paramsEntry) {
		List<ParameterRecord> params = paramsEntry.getValue();
		return params.stream().map(ParameterRecord::getValue).collect(Collectors.toList()).toArray(new String[params.size()]);
	}

}
