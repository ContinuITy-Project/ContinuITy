package org.continuity.session.logs.extractor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.commons.utils.ModularizationUtils;
import org.continuity.idpa.application.Application;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Tobias Angerstein, Henning Schulz
 *
 */
public class ModularizedOPENxtraceSessionLogsExtractor extends OPENxtraceSessionLogsExtractor {

	/**
	 * The hostnames of the services, which have to be tested.
	 */
	private final Map<String, String> services;

	private final Collection<String> targetHostNames;

	private final boolean addPrePostProcessing;

	/**
	 * Constructor.
	 *
	 * @param tag
	 *            The tag of the application
	 * @param eurekaRestTemplate
	 *            eureka rest template
	 * @param hostNames
	 *            The hostnames of the services, which have to be tested.
	 * @param services
	 *            The services to be included
	 * @param addPrePostProcessing
	 *            Whether explicit entries for the pre and post processing should be added to the
	 *            session logs
	 */
	public ModularizedOPENxtraceSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate, Map<String, String> services, boolean addPrePostProcessing) {
		super(tag, eurekaRestTemplate);
		this.services = services;
		this.targetHostNames = ModularizationUtils.getTargetHostNames(services, restTemplate);
		this.addPrePostProcessing = addPrePostProcessing;
	}

	/**
	 * Constructor.
	 *
	 * @param tag
	 *            The tag of the application
	 * @param eurekaRestTemplate
	 *            eureka rest template
	 * @param hostNames
	 *            The hostnames of the services, which have to be tested.
	 * @param services
	 *            The services to be included
	 */
	public ModularizedOPENxtraceSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate, Map<String, String> services) {
		this(tag, eurekaRestTemplate, services, false);
	}

	@Override
	public String getSessionLogs(Iterable<Trace> data) {
		List<HTTPRequestData> httpCallables = extractHttpRequestCallables(data);
		HashMap<String, List<HTTPRequestData>> sortedList = sortBySessionAndTimestamp(httpCallables);
		HashMap<String, Pair<String, String>> businessTransactions = new HashMap<>();

		for (String tag : services.keySet()) {
			Application applicationModel = retrieveApplicationModel(tag);

			if (applicationModel == null) {
				businessTransactions.putAll(getBusinessTransactionsFromOPENxtraces(httpCallables));
			} else {
				businessTransactions.putAll(getBusinessTransactionsFromApplicationModel(applicationModel, httpCallables));
			}
		}
		return getSessionLogsAsString(sortedList, businessTransactions);
	}

	/**
	 * GetRemoteInvocation
	 */

	/**
	 * {@inheritDoc} Has additional condition: Only returns callables, which target one of the
	 * provided hostnames.
	 */
	@Override
	protected List<HTTPRequestData> diveForHTTPRequestProcessingCallable(Callable callable) {
		List<HTTPRequestProcessingImpl> rootCallables = OpenXtraceTracer.forRoot(callable).extractSubtraces();
		List<HTTPRequestData> childCallables = OpenXtraceTracer.forRootAndHosts(callable, targetHostNames).extractSubtraces().stream().map(OPENxtraceHttpRequestData::new).collect(Collectors.toList());

		if (addPrePostProcessing && (rootCallables.size() > 0) && (childCallables.size() > 0)) {
			sortByTimestamp(childCallables);
			HTTPRequestData firstRoot = new OPENxtraceHttpRequestData(rootCallables.get(0));
			HTTPRequestData lastRoot = new OPENxtraceHttpRequestData(rootCallables.get(rootCallables.size() - 1));

			childCallables.add(0, createProcessingRequest(firstRoot, PrePostProcessingHttpRequestData.Mode.PRE));
			childCallables.add(createProcessingRequest(lastRoot, PrePostProcessingHttpRequestData.Mode.POST));
		}

		return childCallables;
	}

	private HTTPRequestData createProcessingRequest(HTTPRequestData root, PrePostProcessingHttpRequestData.Mode mode) {
		// We assume that either the bt is set or it is not important to distinguish between
		// different PRE_ and POST_PROCESSINGs.
		return new PrePostProcessingHttpRequestData(root, mode, null);
	}

}
