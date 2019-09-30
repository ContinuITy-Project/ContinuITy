package org.continuity.cobra.converter;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.continuity.api.entities.ApiFormats;
import org.continuity.cobra.entities.CsvRow;
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
 * Converts {@link CsvRow} to {@link Trace}.
 *
 * @author Henning Schulz
 *
 */
public class CsvRowToOpenXtraceConverter implements OpenXtraceConverter<CsvRow> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvRowToOpenXtraceConverter.class);

	private final boolean hashSessionId;

	public CsvRowToOpenXtraceConverter(boolean hashSessionId) {
		this.hashSessionId = hashSessionId;
	}

	@Override
	public List<Trace> convert(List<CsvRow> data) {
		return data.stream().map(this::convert).collect(Collectors.toList());
	}

	private Trace convert(CsvRow row) {
		TraceImpl trace = new TraceImpl(row.hashCode());
		trace.setRoot(createSubTrace(trace, row));
		return trace;
	}

	private SubTraceImpl createSubTrace(TraceImpl trace, CsvRow row) {
		SubTraceImpl subTrace = new SubTraceImpl(trace.getTraceId(), null, trace);

		LocationImpl location = new LocationImpl(row.getDomain(), Integer.parseInt(row.getPort()), null, null, row.getName());
		subTrace.setLocation(location);

		subTrace.setRoot(createCallable(subTrace, row));

		return subTrace;
	}

	private HTTPRequestProcessingImpl createCallable(SubTraceImpl subTrace, CsvRow row) {
		HTTPRequestProcessingImpl request = new HTTPRequestProcessingImpl(null, subTrace);

		try {
			Date start = ApiFormats.DATE_FORMAT.parse(row.getStartDate());
			Date end = ApiFormats.DATE_FORMAT.parse(row.getEndDate());

			request.setTimestamp(start.getTime());
			request.setResponseTime((end.getTime() - start.getTime()) * MILLIS_TO_NANOS);
		} catch (ParseException e) {
			LOGGER.error("Could not parse timestamp!", e);
		}
		request.setUri(row.getPath());
		request.setRequestMethod(HTTPMethod.valueOf(row.getMethod().toUpperCase()));
		request.setHTTPParameters(WebUtils.formatQueryParameters(row.getParameters()));

		OPENxtraceUtils.setSessionId(request, hashSessionId ? DigestUtils.sha256Hex(row.getSessionId()) : row.getSessionId());

		return request;
	}

}
