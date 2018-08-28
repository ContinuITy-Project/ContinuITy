package org.continuity.session.logs.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.rest.InspectITRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;

/**
 * Converts invocation sequences into session logs.
 *
 * @author Alper Hidiroglu, Jonas Kunz, Tobias Angerstein, Henning Schulz
 *
 */
public class InspectITSessionLogsExtractor extends AbstractSessionLogsExtractor<InvocationSequenceData> {

	private static final Logger LOGGER = LoggerFactory.getLogger(InspectITSessionLogsExtractor.class);

	/**
	 * CMR configuration
	 */
	private String cmrConfig;

	/**
	 * Constructor
	 * 
	 * @param tag
	 *            the tag of the application
	 * @param eurekaRestTemplate
	 *            eureka rest template
	 * @param cmrConfig
	 */
	public InspectITSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate, String cmrConfig) {
		super(tag, eurekaRestTemplate);
		this.cmrConfig = cmrConfig;
	}

	/**
	 * Converts the specified invocation sequences to session logs, naming it as specified in the
	 * business transactions.
	 *
	 * @param invocationSequences
	 *            The invocation sequences to be processed.
	 * @param businessTransactions
	 *            The business transactions consisting of the business transaction name and the
	 *            abstract URI (e.g., <code>/foo/{bar}/get</code>).
	 * @return The session logs as string.
	 */
	public String getSessionLogs(Iterable<InvocationSequenceData> invocationSequences) {

		HashMap<String, List<HTTPRequestData>> sortedList = sortBySessionAndTimestamp(invocationSequences);

		Application systemModel = retrieveApplicationModel(tag);
		HashMap<Long, Pair<String, String>> businessTransactions;

		if (systemModel == null) {
			businessTransactions = getBusinessTransactionsFromInspectitBTs(invocationSequences);
		} else {
			businessTransactions = getBusinessTransactionsFromApplicationModel(systemModel, invocationSequences);
		}

		return getSessionLogsAsString(sortedList, businessTransactions);
	}

	/**
	 * Returns all {@link HttpTimerData} of the traces sorted by session and timestamp
	 * 
	 * @param invocationSequences
	 *            the input traces
	 * @return Map of session logs and the corresponding requests represented as
	 *         {@link HttpTimerData}
	 */
	private HashMap<String, List<HTTPRequestData>> sortBySessionAndTimestamp(Iterable<InvocationSequenceData> invocationSequences) {

		HashMap<String, List<HTTPRequestData>> sortedSessionsInvoc = new HashMap<String, List<HTTPRequestData>>();

		ArrayList<HttpTimerData> sortedList = new ArrayList<HttpTimerData>();

		// Only InvocationSequenceData with SessionID != null
		for (InvocationSequenceData invoc : invocationSequences) {
			if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
				HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
				sortedList.add(dat);
			}
		}

		Collections.sort(sortedList, new Comparator<HttpTimerData>() {
			@Override
			public int compare(HttpTimerData data1, HttpTimerData data2) {
				int startTimeComparison = Long.compare(data1.getTimeStamp().getTime(), data2.getTimeStamp().getTime());

				if (startTimeComparison != 0) {
					return startTimeComparison;
				} else {
					return Double.compare(data1.getDuration(), data2.getDuration());
				}
			}
		});

		String firstSessionId = null;
		int firstSessionIdIndex = 0;

		for (HttpTimerData invoc : sortedList) {
			firstSessionId = extractSessionIdFromCookies(invoc.getHeaders().get("cookie"));

			if (firstSessionId != null) {
				break;
			}

			firstSessionIdIndex++;
		}

		int i = 0;

		for (HttpTimerData invoc : sortedList) {
			String sessionId;

			if (i < firstSessionIdIndex) {
				sessionId = firstSessionId;
			} else {
				sessionId = extractSessionIdFromCookies(invoc.getHeaders().get("cookie"));
			}

			if (sessionId != null) {
				if (sortedSessionsInvoc.containsKey(sessionId)) {
					sortedSessionsInvoc.get(sessionId).add(new InspectITHttpRequestData(invoc));
				} else {
					List<HTTPRequestData> newList = new ArrayList<HTTPRequestData>();
					newList.add(new InspectITHttpRequestData(invoc));
					sortedSessionsInvoc.put(sessionId, newList);
				}
			}

			i++;
		}
		return sortedSessionsInvoc;
	}

	/**
	 * Extracts all different businessTransactions from the input traces
	 * 
	 * @param invocationSequences
	 *            the input traces
	 * @return the business transactions
	 * 
	 */
	private HashMap<Long, Pair<String, String>> getBusinessTransactionsFromInspectitBTs(Iterable<InvocationSequenceData> invocationSequences) {
		InspectITRestClient fetcher = new InspectITRestClient(cmrConfig);

		Iterable<ApplicationData> applications = null;
		Iterable<BusinessTransactionData> justOneMonitoredApplication = null;

		try {
			applications = fetcher.fetchAllApplications();

			for (ApplicationData application : (List<ApplicationData>) applications) {
				// TODO: This comparison does not work (because application is not a string)!
				// @Tobias: Should be solved now ...
				if (!application.getName().equals("Unknown Application")) {
					justOneMonitoredApplication = fetcher.fetchAllBusinessTransactions(application.getId());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		HashMap<Integer, String> businessTransactionsMap = new HashMap<Integer, String>();

		for (BusinessTransactionData transaction : justOneMonitoredApplication) {
			int businessTransactionId = transaction.getId();
			String businessTransactionName = transaction.getName();
			businessTransactionsMap.put(businessTransactionId, businessTransactionName);
		}

		HashMap<Long, Pair<String, String>> businessTransactions = new HashMap<>();

		for (InvocationSequenceData invoc : invocationSequences) {
			if (businessTransactionsMap.get(invoc.getBusinessTransactionId()) != null) {
				String businessTransactionName = businessTransactionsMap.get(invoc.getBusinessTransactionId());

				if (!businessTransactionName.equals("Unknown Transaction")) {
					if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
						HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
						businessTransactions.put(dat.getId(), Pair.of(businessTransactionName, dat.getHttpInfo().getUri()));
					}
				}
			}
		}

		return businessTransactions;
	}

	/**
	 * Returns all business transactions of the application model, which are consisted in the
	 * invocationSequences.
	 * 
	 * @param application
	 *            the application
	 * @param invocationSequences
	 *            the input traces
	 * @return the business transactions
	 */
	private HashMap<Long, Pair<String, String>> getBusinessTransactionsFromApplicationModel(Application application, Iterable<InvocationSequenceData> invocationSequences) {
		HashMap<Long, Pair<String, String>> businessTransactions = new HashMap<>();
		RequestUriMapper uriMapper = new RequestUriMapper(application);

		for (InvocationSequenceData invoc : invocationSequences) {
			if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
				HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
				HttpEndpoint interf = uriMapper.map(dat.getHttpInfo().getUri(), dat.getHttpInfo().getRequestMethod());

				if ((interf != null) && interf.getDomain().equals(dat.getHttpInfo().getServerName()) && interf.getPort().equals(Integer.toString(dat.getHttpInfo().getServerPort()))) {
					businessTransactions.put(dat.getId(), Pair.of(interf.getId(), interf.getPath()));
				}
			}
		}

		return businessTransactions;
	}
}
