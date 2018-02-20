package org.continuity.session.logs.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.workload.dsl.RequestUriMapper;
import org.continuity.rest.InspectITRestClient;
import org.continuity.session.logs.converter.SessionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITAbstractCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSpanCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITTraceImpl;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import rocks.inspectit.server.open.xtrace.OPENxtraceDeserializer;
import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;

/**
 *
 * @author Alper Hi, Tobias Angerstein
 *
 */
public class SessionLogsPipelineManager {

	private static String CMRCONFIG;
	private static final boolean USEOPENxtrace = Boolean.getBoolean(System.getProperty("USE_OPEN_XTRACE", "true"));

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsPipelineManager.class);

	private String link;
	private String tag;

	private final RestTemplate plainRestTemplate;

	private final RestTemplate eurekaRestTemplate;

	public SessionLogsPipelineManager(String link, String tag, RestTemplate plainRestTemplate, RestTemplate eurekaRestTemplate) {
		this.link = link;
		this.tag = tag;
		UriComponents uri = UriComponentsBuilder.fromHttpUrl(link).build();
		CMRCONFIG = uri.getHost() + ":" + uri.getPort();
		this.plainRestTemplate = plainRestTemplate;
		this.eurekaRestTemplate = eurekaRestTemplate;
	}

	/**
	 * Runs the pipeline
	 *
	 * @return
	 */
	public String runPipeline() {
		if (USEOPENxtrace) {
			return getSessionLogs(this.link);
		} else {
			return getSessionLogsUsingInvocationSequences();
		}
	}

	/**
	 * Gets session logs without using the invocation sequences of the CMR.
	 *
	 * @return
	 */
	private String getSessionLogsUsingInvocationSequences() {
		InspectITRestClient fetcher = new InspectITRestClient(CMRCONFIG);
		MultiValueMap<String, String> uriParameters = UriComponentsBuilder.fromHttpUrl(this.link).build().getQueryParams();
		Iterable<InvocationSequenceData> invocationSequenceIterable = fetcher.fetchAll(0, uriParameters.getFirst("fromDate"), uriParameters.getFirst("toDate"));
		return convertInvocationSequencesIntoSessionLogs(invocationSequenceIterable);
	}

	/**
	 * Gets Session logs using OPEN.xtrace
	 */
	public String getSessionLogs(String link) {
		String openxtrace = this.plainRestTemplate.getForObject(link, String.class);
		OPENxtraceDeserializer deserializer = new OPENxtraceDeserializer();
		try {
			List<IITTraceImpl> traceList = deserializer.deserialize(openxtrace);
			List<InvocationSequenceData> extractedInvocationSequences = new ArrayList<InvocationSequenceData>();
			for (IITTraceImpl traceImpl : traceList) {
				Callable callable = traceImpl.getRoot().getRoot();
				if ((callable instanceof IITRemoteInvocation) || (callable instanceof IITHTTPRequestProcessing)) {
					extractedInvocationSequences.add(((IITAbstractCallable) callable).getInvocationSequenceData());
				} else if (callable instanceof IITSpanCallable) {
					// TODO: invoke a recursive method, which build the session logs based on the
					// span information.
				}
			}

			return convertInvocationSequencesIntoSessionLogs(extractedInvocationSequences);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private String convertInvocationSequencesIntoSessionLogs(Iterable<InvocationSequenceData> invocationSequences) {
		SystemModel systemModel = retrieveSystemModel();
		HashMap<Long, String> businessTransactions;

		if (systemModel == null) {
			businessTransactions = getBusinessTransactionsFromInspectitBTs(invocationSequences);
		} else {
			businessTransactions = getBusinessTransactionsFromSystemModel(systemModel, invocationSequences);
		}

		SessionConverter converter = new SessionConverter();

		return converter.convertIntoSessionLog(invocationSequences, businessTransactions);
	}

	private SystemModel retrieveSystemModel() {
		if (tag == null) {
			LOGGER.warn("Cannot retrieve the system model for naming the Session Logs. The tag is nulL!");
			return null;
		}

		try {
			return eurekaRestTemplate.getForObject("http://system-model/system/" + tag, SystemModel.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Received error status code when asking for system model with tag " + tag, e);
			return null;
		}
	}

	private HashMap<Long, String> getBusinessTransactionsFromInspectitBTs(Iterable<InvocationSequenceData> invocationSequences) {
		InspectITRestClient fetcher = new InspectITRestClient(CMRCONFIG);

		Iterable<ApplicationData> applications = null;
		Iterable<BusinessTransactionData> justOneMonitoredApplication = null;

		try {
			applications = fetcher.fetchAllApplications();

			for (ApplicationData application : (List<ApplicationData>) applications) {
				// TODO: This comparison does not work (because application is not a string)!
				if (!application.equals("Unknown Application")) {
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

		HashMap<Long, String> businessTransactions = new HashMap<Long, String>();

		for (InvocationSequenceData invoc : invocationSequences) {
			if (businessTransactionsMap.get(invoc.getBusinessTransactionId()) != null) {
				String businessTransactionName = businessTransactionsMap.get(invoc.getBusinessTransactionId());

				if (!businessTransactionName.equals("Unknown Transaction")) {
					if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
						HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
						businessTransactions.put(dat.getId(), businessTransactionName);
					}
				}
			}
		}

		return businessTransactions;
	}

	private HashMap<Long, String> getBusinessTransactionsFromSystemModel(SystemModel system, Iterable<InvocationSequenceData> invocationSequences) {
		HashMap<Long, String> businessTransactions = new HashMap<Long, String>();
		RequestUriMapper uriMapper = new RequestUriMapper(system);

		for (InvocationSequenceData invoc : invocationSequences) {
			if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
				HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
				HttpInterface interf = uriMapper.map(dat.getHttpInfo().getUri(), dat.getHttpInfo().getRequestMethod());

				if (interf != null) {
					businessTransactions.put(dat.getId(), interf.getId());
				}
			}
		}

		return businessTransactions;
	}

}
