package org.continuity.session.logs.extractor;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.spec.research.open.xtrace.api.core.Location;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;
import org.springframework.web.client.RestTemplate;

/**
 * Extracts session logs from OPEN.xtraces
 *
 * @author Tobias Angerstein
 *
 */
public class OPENxtraceSessionLogsExtractor extends AbstractSessionLogsExtractor<Trace> {

	/**
	 * Constructor
	 *
	 * @param tag
	 *            tag of the application
	 * @param eurekaRestTemplate
	 */
	public OPENxtraceSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate) {
		super(tag, eurekaRestTemplate);
	}

	@Override
	public String getSessionLogs(Iterable<Trace> data) {
		List<HTTPRequestProcessingImpl> httpCallables = extractHttpRequestCallables(data);
		HashMap<String, List<HTTPRequestData>> sortedList = sortBySessionAndTimestamp(httpCallables);
		Application applicationModel = retrieveApplicationModel(tag);
		HashMap<Long, Pair<String, String>> businessTransactions;

		if (applicationModel == null) {
			businessTransactions = getBusinessTransactionsFromOPENxtraces(httpCallables);
		} else {
			businessTransactions = getBusinessTransactionsFromApplicationModel(applicationModel, httpCallables);
		}

		return getSessionLogsAsString(sortedList, businessTransactions);
	}

	/**
	 * Returns all {@link HTTPRequestProcessingImpl} of the traces sorted by session and timestamp
	 *
	 * @param sortedHTTPInvocCallables
	 *            all httpRequestProcessingCallables
	 *
	 * @return Map of session logs and the corresponding requests, which are represented as
	 *         {@link HTTPRequestProcessingImpl}
	 */
	protected HashMap<String, List<HTTPRequestData>> sortBySessionAndTimestamp(List<HTTPRequestProcessingImpl> sortedHTTPInvocCallables) {
		Collections.sort(sortedHTTPInvocCallables, new Comparator<HTTPRequestProcessingImpl>() {

			@Override
			public int compare(HTTPRequestProcessingImpl arg0, HTTPRequestProcessingImpl arg1) {
				int startTimeComparison = Long.compare(arg0.getTimestamp(), arg1.getTimestamp());

				if (startTimeComparison != 0) {
					return startTimeComparison;
				} else {
					return Double.compare(arg0.getResponseTime(), arg1.getResponseTime());
				}
			}
		});

		HashMap<String, List<HTTPRequestData>> sessionRequestMap = new HashMap<String, List<HTTPRequestData>>();

		for (HTTPRequestProcessingImpl requestData : sortedHTTPInvocCallables) {
			if (requestData.getHTTPHeaders().isPresent() && requestData.getHTTPHeaders().get().containsKey("cookie")) {
				String sessionId = extractSessionIdFromCookies(requestData.getHTTPHeaders().get().get("cookie"));
				if (null != sessionId) {
					if (sessionRequestMap.containsKey(sessionId)) {
						sessionRequestMap.get(sessionId).add(new OPENxtraceHttpRequestData(requestData));
					} else {
						sessionRequestMap.put(sessionId, new ArrayList<HTTPRequestData>(Arrays.asList(new OPENxtraceHttpRequestData(requestData))));
					}
				}
			}
		}

		return sessionRequestMap;
	}

	/**
	 * Extracts all httpRequestProcessingImpl callables
	 *
	 * @param traces
	 * @param sortedHTTPInvocCallables
	 */
	protected List<HTTPRequestProcessingImpl> extractHttpRequestCallables(Iterable<Trace> traces) {
		List<HTTPRequestProcessingImpl> httpCallables = new ArrayList<HTTPRequestProcessingImpl>();
		for (Trace trace : traces) {
			if ((null != trace.getRoot()) && (null != trace.getRoot().getRoot())) {
				httpCallables.addAll(diveForHTTPRequestProcessingCallable(trace.getRoot().getRoot()));
			}
		}
		return httpCallables;
	}

	/**
	 * Returns the {@link HTTPRequestProcessingImpl} object of the corresponding
	 * {@link RemoteInvocationImpl} which is on the highest level.
	 *
	 * @param callable
	 *            The root callable
	 * @return List of {@link HTTPRequestProcessingImpl}
	 */
	protected List<HTTPRequestProcessingImpl> diveForHTTPRequestProcessingCallable(Callable callable) {
		return OpenXtraceTracer.forRoot(callable).extractSubtraces();
	}

	/**
	 * Extracts all different businessTransactions from the input traces Assumption: applicationName
	 * and tag name are identical Assumption: Each Containing Subtrace of the first found
	 * HTTPRequestProcessingCallable consists of business transaction information.
	 *
	 * @param traces
	 *            input traces
	 *
	 * @return all existing business transactions
	 */
	protected HashMap<Long, Pair<String, String>> getBusinessTransactionsFromOPENxtraces(Iterable<HTTPRequestProcessingImpl> httpCallables) {
		HashMap<Long, Pair<String, String>> businessTransactions = new HashMap<Long, Pair<String, String>>();

		for (HTTPRequestProcessingImpl httpCallable : httpCallables) {
			if ((httpCallable.getContainingSubTrace() != null) && (httpCallable.getContainingSubTrace().getLocation() != null)) {
				Location traceLocation = httpCallable.getContainingSubTrace().getLocation();

				if (traceLocation.getBusinessTransaction().isPresent() && !traceLocation.getBusinessTransaction().get().equals("unkown transaction")) {
					businessTransactions.put((Long) httpCallable.getIdentifier().get(), Pair.of(traceLocation.getBusinessTransaction().get(), httpCallable.getUri()));
				}
			}
		}

		return businessTransactions;
	}

	/**
	 * Returns all business transactions of the application model, which are consisted in the
	 * httpCallables.
	 *
	 * @param application
	 *            the application model
	 * @param httpCallables
	 *            the {@link HTTPRequestProcessingImpl} callables
	 * @return
	 */
	protected HashMap<Long, Pair<String, String>> getBusinessTransactionsFromApplicationModel(Application application, Iterable<HTTPRequestProcessingImpl> httpCallables) {
		HashMap<Long, Pair<String, String>> businessTransactions = new HashMap<Long, Pair<String, String>>();
		RequestUriMapper uriMapper = new RequestUriMapper(application);

		for (HTTPRequestProcessingImpl httpCallable : httpCallables) {
			HttpEndpoint interf = uriMapper.map(httpCallable.getUri(), httpCallable.getRequestMethod().get().name());

			if ((interf != null)
					&& interf.getDomain().equals(httpCallable.getContainingSubTrace().getLocation().getHost())
					&& interf.getPort().equals(Integer.toString(httpCallable.getContainingSubTrace().getLocation().getPort()))) {
				businessTransactions.put((Long) httpCallable.getIdentifier().get(), Pair.of(interf.getId(), interf.getPath()));
			}

		}

		return businessTransactions;

	}
}
