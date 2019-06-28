package org.continuity.session.logs.extractor;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
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
	 * @param aid
	 *            app-id of the application
	 * @param eurekaRestTemplate
	 */
	public OPENxtraceSessionLogsExtractor(AppId aid, RestTemplate eurekaRestTemplate) {
		super(aid, eurekaRestTemplate);
	}

	@Override
	public String getSessionLogs(Iterable<Trace> data) {
		List<HTTPRequestData> httpCallables = extractHttpRequestCallables(data);
		HashMap<String, List<HTTPRequestData>> sortedList = sortBySessionAndTimestamp(httpCallables);
		Application applicationModel = retrieveApplicationModel(aid);
		HashMap<String, Pair<String, String>> businessTransactions;

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
	protected HashMap<String, List<HTTPRequestData>> sortBySessionAndTimestamp(List<HTTPRequestData> sortedHTTPInvocCallables) {
		sortByTimestamp(sortedHTTPInvocCallables);

		HashMap<String, List<HTTPRequestData>> sessionRequestMap = new HashMap<String, List<HTTPRequestData>>();

		for (HTTPRequestData requestData : sortedHTTPInvocCallables) {
			String sessionId = requestData.getSessionId();

			if ((sessionId != null) && !sessionId.isEmpty()) {
				if (sessionRequestMap.containsKey(sessionId)) {
					sessionRequestMap.get(sessionId).add(requestData);
				} else {
					sessionRequestMap.put(sessionId, new ArrayList<>(Arrays.asList(requestData)));
				}
			}
		}

		return sessionRequestMap;
	}

	protected void sortByTimestamp(List<HTTPRequestData> requests) {
		Collections.sort(requests, new Comparator<HTTPRequestData>() {

			@Override
			public int compare(HTTPRequestData arg0, HTTPRequestData arg1) {
				int startTimeComparison = Long.compare(arg0.getTimestamp(), arg1.getTimestamp());

				if (startTimeComparison != 0) {
					return startTimeComparison;
				} else {
					return Double.compare(arg0.getResponseTime(), arg1.getResponseTime());
				}
			}
		});
	}

	/**
	 * Extracts all httpRequestProcessingImpl callables
	 *
	 * @param traces
	 * @param sortedHTTPInvocCallables
	 */
	protected List<HTTPRequestData> extractHttpRequestCallables(Iterable<Trace> traces) {
		List<HTTPRequestData> httpCallables = new ArrayList<HTTPRequestData>();
		for (Trace trace : traces) {
			if ((null != trace.getRoot()) && (null != trace.getRoot().getRoot())) {
				httpCallables.addAll(diveForHTTPRequestProcessingCallable(trace.getRoot().getRoot()));
			}
		}
		return httpCallables;
	}

	/**
	 * Returns a list of {@link HTTPRequestData} for the {@link HTTPRequestProcessingImpl} object of
	 * the corresponding {@link RemoteInvocationImpl} which is on the highest level.
	 *
	 * @param callable
	 *            The root callable
	 * @return List of {@link HTTPRequestData}
	 */
	protected List<HTTPRequestData> diveForHTTPRequestProcessingCallable(Callable callable) {
		return OpenXtraceTracer.forRoot(callable).extractSubtraces().stream().map(OPENxtraceHttpRequestData::new).collect(Collectors.toList());
	}

	/**
	 * Extracts all different businessTransactions from the input traces Assumption: applicationName
	 * and app-id name are identical Assumption: Each Containing Subtrace of the first found
	 * HTTPRequestProcessingCallable consists of business transaction information.
	 *
	 * @param traces
	 *            input traces
	 *
	 * @return all existing business transactions
	 */
	protected HashMap<String, Pair<String, String>> getBusinessTransactionsFromOPENxtraces(Iterable<HTTPRequestData> httpCallables) {
		HashMap<String, Pair<String, String>> businessTransactions = new HashMap<>();

		for (HTTPRequestData httpCallable : httpCallables) {
			String businessTransaction = httpCallable.getBusinessTransaction();

			if ((businessTransaction != null) && !businessTransaction.isEmpty() && !businessTransaction.equals("unkown transaction")) {
				businessTransactions.put(httpCallable.getIdentifier(), Pair.of(businessTransaction, httpCallable.getUri()));
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
	protected HashMap<String, Pair<String, String>> getBusinessTransactionsFromApplicationModel(Application application, Iterable<HTTPRequestData> httpCallables) {
		HashMap<String, Pair<String, String>> businessTransactions = new HashMap<>();
		RequestUriMapper uriMapper = new RequestUriMapper(application);

		for (HTTPRequestData httpCallable : httpCallables) {
			String bt = null;
			String path = null;

			if (httpCallable.isSpecial()) {
				bt = httpCallable.getBusinessTransaction();
				path = httpCallable.getUri();
			} else {
				HttpEndpoint interf = uriMapper.map(httpCallable.getUri(), httpCallable.getRequestMethod());

				if ((interf != null) && interf.getDomain().equals(httpCallable.getHost()) && interf.getPort().equals(Integer.toString(httpCallable.getPort()))) {
					bt = interf.getId();
					path = interf.getPath();
				}
			}

			if (bt != null) {
				businessTransactions.put(httpCallable.getIdentifier(), Pair.of(bt, path));
			}
		}

		return businessTransactions;

	}
}
