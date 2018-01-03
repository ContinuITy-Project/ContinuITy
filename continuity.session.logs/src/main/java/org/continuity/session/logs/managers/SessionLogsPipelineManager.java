package org.continuity.session.logs.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.continuity.rest.InspectITRestClient;
import org.continuity.session.logs.converter.SessionConverter;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITAbstractCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITHTTPRequestProcessing;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITRemoteInvocation;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITSpanCallable;
import org.spec.research.open.xtrace.adapters.inspectit.impl.IITTraceImpl;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.springframework.web.client.RestTemplate;

import rocks.inspectit.server.open.xtrace.OPENxtraceDeserializer;
import rocks.inspectit.shared.all.cmr.model.MethodIdent;
import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
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

	private static final String AGENTNAME = System.getProperty("AGENT_NAME", "dvdstore");

	private static final String CMRCONFIG = System.getProperty("CMR_CONFIG", "letslx037:8182");

	private String link;

	private RestTemplate restTemplate = new RestTemplate();

	public SessionLogsPipelineManager(String link) {
		this.link = link;
	}

	/**
	 * Runs the pipeline
	 * 
	 * @return
	 */
	public String runPipeline() {
		return getSessionLogs(this.link);
	}

	/**
	 * Gets OPEN.xtrace
	 */
	public String getSessionLogs(String link) {
		String openxtrace = this.restTemplate.getForObject(link, String.class);
		OPENxtraceDeserializer deserializer = new OPENxtraceDeserializer();
		try {
			List<IITTraceImpl> traceList = deserializer.deserialize(openxtrace);
			List<InvocationSequenceData> extractedInvocationSequences = new ArrayList<InvocationSequenceData>();
			for (IITTraceImpl traceImpl : traceList) {
				Callable callable = traceImpl.getRoot().getRoot();
				if (callable instanceof IITRemoteInvocation || callable instanceof IITHTTPRequestProcessing) {
					extractedInvocationSequences.add(((IITAbstractCallable) callable).getInvocationSequenceData());
				} else if (callable instanceof IITSpanCallable) {
					// TODO: invoke a recursive method, which build the session logs based on the
					// span information.
				}
			}

			return convertOpenXtraceIntoSessionLogs(extractedInvocationSequences);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private String convertOpenXtraceIntoSessionLogs(List<InvocationSequenceData> invocationSequences) {

		InspectITRestClient fetcher = new InspectITRestClient(CMRCONFIG);

		PlatformIdent agent;

		Map<Long, MethodIdent> methods = new HashMap<>();
		try {
			agent = StreamSupport.stream(fetcher.fetchAllAgents().spliterator(), false).filter((a) -> a.getAgentName().equalsIgnoreCase(AGENTNAME)).findFirst().get();

			for (MethodIdent method : fetcher.fetchAllMethods(agent.getId())) {
				methods.put(method.getId(), method);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Iterable<ApplicationData> applications = null;
		Iterable<BusinessTransactionData> justOneMonitoredApplication = null;

		try {
			applications = fetcher.fetchAllApplications();

			for (ApplicationData application : (List<ApplicationData>) applications) {
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

		SessionConverter converter = new SessionConverter();

		return converter.convertIntoSessionLog(methods, agent, invocationSequences, businessTransactions);
	}
}
