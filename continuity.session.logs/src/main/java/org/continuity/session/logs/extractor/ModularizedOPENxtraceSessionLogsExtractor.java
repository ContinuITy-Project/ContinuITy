package org.continuity.session.logs.extractor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.pdfbox.pdmodel.MissingResourceException;
import org.continuity.api.entities.artifact.ModularizedSessionLogs;
import org.continuity.api.entities.artifact.ProcessingTimeNormalDistributions;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.commons.utils.ModularizationUtils;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.api.core.callables.NestingCallable;
import org.spec.research.open.xtrace.api.core.callables.RemoteInvocation;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.web.client.RestTemplate;

public class ModularizedOPENxtraceSessionLogsExtractor extends OPENxtraceSessionLogsExtractor {
	private static final int TO_MILLIS_DIVIDER = 1000000;

	/**
	 * The hostnames of the services, which have to be tested.
	 */
	private final Map<String, String> services;

	private final Collection<String> targetHostNames;

	/**
	 * The different application models per tag
	 */
	private Map<String, Application> applicationModels = new HashMap<String, Application>();

	/**
	 * Constructor.
	 *
	 * @param tag
	 *            The tag of the application
	 * @param eurekaRestTemplate
	 *            eureka rest template
	 * @param hostNames
	 *            The hostnames of the services, which have to be tested.
	 */
	public ModularizedOPENxtraceSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate, Map<String, String> services) {
		super(tag, eurekaRestTemplate);
		this.services = services;
		this.targetHostNames = ModularizationUtils.getTargetHostNames(services, restTemplate);
	}

	@Override
	public String getSessionLogs(Iterable<Trace> data) {
		List<HTTPRequestProcessingImpl> httpCallables = extractHttpRequestCallables(data);
		HashMap<String, List<HTTPRequestData>> sortedList = sortBySessionAndTimestamp(httpCallables);
		HashMap<Long, Pair<String, String>> businessTransactions = new HashMap<Long, Pair<String, String>>();

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
	 * Extract thinktimes from traces
	 *
	 * @param traces
	 *            the traces
	 * @return thinktime distributions before and after the modularized callable is executed.
	 */
	protected Map<String, ProcessingTimeNormalDistributions> extractThinkTimesFromTraces(Iterable<Trace> traces) {
		HashMap<String, List<Double>> preThinkTimes = new HashMap<String, List<Double>>();
		HashMap<String, List<Double>> postThinkTimes = new HashMap<String, List<Double>>();
		for (Trace trace : traces) {
			if ((null != trace.getRoot()) && (null != trace.getRoot().getRoot())) {
				try {
					Triple<String, Long, Long> thinktimes = getThinktimes(trace.getRoot().getRoot());

					if (preThinkTimes.containsKey(thinktimes.getLeft())) {
						preThinkTimes.get(thinktimes.getLeft()).add(thinktimes.getMiddle().doubleValue());
					} else {
						preThinkTimes.put(thinktimes.getLeft(), new ArrayList<Double>(Arrays.asList(thinktimes.getMiddle().doubleValue())));
					}

					if (postThinkTimes.containsKey(thinktimes.getLeft())) {
						postThinkTimes.get(thinktimes.getLeft()).add(thinktimes.getRight().doubleValue());
					} else {
						postThinkTimes.put(thinktimes.getLeft(), new ArrayList<Double>(Arrays.asList(thinktimes.getRight().doubleValue())));
					}
				} catch (MissingResourceException e) {
					e.printStackTrace();
				}
			}
		}

		return preThinkTimes.keySet().stream()
				.map(endpointName -> new AbstractMap.SimpleEntry<>(endpointName, new ProcessingTimeNormalDistributions(preThinkTimes.get(endpointName), postThinkTimes.get(endpointName))))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public ModularizedSessionLogs getSessionLogsAndThinkTimes(Iterable<Trace> data) {
		List<HTTPRequestProcessingImpl> httpCallables = extractHttpRequestCallables(data);
		HashMap<String, List<HTTPRequestData>> sortedList = sortBySessionAndTimestamp(httpCallables);
		HashMap<Long, Pair<String, String>> businessTransactions = new HashMap<Long, Pair<String, String>>();

		for (String tag : services.keySet()) {
			Application applicationModel = retrieveApplicationModel(tag);

			if (applicationModel == null) {
				businessTransactions.putAll(getBusinessTransactionsFromOPENxtraces(httpCallables));
			} else {
				applicationModels.put(tag, applicationModel);
				businessTransactions.putAll(getBusinessTransactionsFromApplicationModel(applicationModel, httpCallables));
			}
		}

		Map<String, ProcessingTimeNormalDistributions> normalDistributions = extractThinkTimesFromTraces(data);

		ModularizedSessionLogs sessionlogs = new ModularizedSessionLogs(normalDistributions);
		sessionlogs.setLogs(getSessionLogsAsString(sortedList, businessTransactions));
		sessionlogs.setDataTimestamp(new Date());
		return sessionlogs;
	}

	/**
	 * GetRemoteInvocation
	 */

	/**
	 * {@inheritDoc} Has additional condition: Only returns callables, which target one of the
	 * provided hostnames.
	 */
	@Override
	protected List<HTTPRequestProcessingImpl> diveForHTTPRequestProcessingCallable(Callable callable) {
		return OpenXtraceTracer.forRootAndHosts(callable, targetHostNames).extractSubtraces();
	}

	/**
	 * TODO: Only used by {@link #getThinktimes(Callable)}. To be replaced.
	 *
	 * Checks, whether the callable targets the dedicated host.
	 *
	 * @param httpCallable
	 *            The {@link HTTPRequestProcessingImpl}, which is analyzed.
	 * @return true, if it targets one of the dedicated hosts, else false
	 * @throws MissingResourceException
	 *
	 */
	private boolean callableTargetsDedicatedHost(HTTPRequestProcessingImpl httpCallable) throws MissingResourceException {
		if (!services.values().contains("undefined")) {
			return services.values().contains(httpCallable.getContainingSubTrace().getLocation().getHost());
		} else {
			for (String tag : services.keySet()) {
				Application applicationModel = retrieveApplicationModel(tag);
				if (applicationModel == null) {
					throw new MissingResourceException("No application model found. Please either define an application model or provide a hostname");
				} else {
					RequestUriMapper uriMapper = new RequestUriMapper(applicationModel);
					HttpEndpoint interf = uriMapper.map(httpCallable.getUri(), httpCallable.getRequestMethod().get().name());

					if ((interf != null) && interf.getDomain().equals(httpCallable.getContainingSubTrace().getLocation().getHost())) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * Returns the corresponding pre and post thinktimes: Since the root node has a exclusive
	 * response time, the difference between the start of the root callable and the start of the
	 * modularized callable has to be checked. This information can be later used to adapt the
	 * thinktimes of the modularized behavior model. Same stands for the difference of the exit
	 * times.
	 *
	 * @param callable
	 *            the callable tree, which need to be scanned
	 * @return a pair of pre thinktime and post thinktime in nano second
	 * @throws MissingResourceException
	 */
	private Triple<String, Long, Long> getThinktimes(Callable callable) throws MissingResourceException {
		HTTPRequestProcessingImpl rootHttpCallable = null;
		HTTPRequestProcessingImpl modularizedHttpCallable = null;

		if (null != callable) {
			LinkedBlockingQueue<Callable> callables = new LinkedBlockingQueue<Callable>();
			callables.add(callable);
			boolean foundRootHttpCallable = false;
			while (!callables.isEmpty()) {
				Callable currentCallable = callables.poll();
				if (currentCallable instanceof HTTPRequestProcessingImpl) {
					if (!foundRootHttpCallable) {
						rootHttpCallable = (HTTPRequestProcessingImpl) currentCallable;
						foundRootHttpCallable = true;
					} else if (foundRootHttpCallable && callableTargetsDedicatedHost((HTTPRequestProcessingImpl) currentCallable)) {
						modularizedHttpCallable = (HTTPRequestProcessingImpl) currentCallable;
						break;
					}
				}
				if (currentCallable instanceof NestingCallable) {
					callables.addAll(((NestingCallable) currentCallable).getCallees());
				} else if ((currentCallable instanceof RemoteInvocation) && ((RemoteInvocation) currentCallable).getTargetSubTrace().isPresent()
						&& (null != ((RemoteInvocation) currentCallable).getTargetSubTrace().get().getRoot())) {
					callables.add(((RemoteInvocation) currentCallable).getTargetSubTrace().get().getRoot());
				}
			}
		}
		return calculateThinktimes(rootHttpCallable, modularizedHttpCallable);
	}

	/**
	 * Returns a pair of the pre think time and the post think time
	 *
	 * @param rootHttpCallable
	 *            the original http request
	 * @param modularizedHttpCallable
	 *            the modularized http request
	 * @return a {@link Triple} of pre think time and the post think time in nano seconds
	 */
	private Triple<String, Long, Long> calculateThinktimes(HTTPRequestProcessingImpl rootHttpCallable, HTTPRequestProcessingImpl modularizedHttpCallable) {

		if ((null == rootHttpCallable) && (null == modularizedHttpCallable)) {
			return null;
		} else if (((null != rootHttpCallable) && (null == modularizedHttpCallable))) {
			// No modularized request, which targets the right system, was found. In this case, we
			// need to provide the whole response time of the service
			return Triple.of("root", rootHttpCallable.getResponseTime() / TO_MILLIS_DIVIDER /2, rootHttpCallable.getResponseTime() / TO_MILLIS_DIVIDER /2);
		} else {
			HttpEndpoint endpoint = null;
			for (Application application : applicationModels.values()) {
				RequestUriMapper uriMapper = new RequestUriMapper(application);
				endpoint = uriMapper.map(modularizedHttpCallable.getUri(), modularizedHttpCallable.getRequestMethod().get().name());

				if (endpoint != null) {
					break;
				}
			}
			if (endpoint != null) {
				return Triple.of(endpoint.getId(), (modularizedHttpCallable.getTimestamp() - rootHttpCallable.getTimestamp()), (rootHttpCallable.getExitTime() - modularizedHttpCallable.getExitTime()));
			} else {
				return Triple.of("root", rootHttpCallable.getResponseTime() / TO_MILLIS_DIVIDER /2, rootHttpCallable.getResponseTime() / TO_MILLIS_DIVIDER /2);
			}
		}
	}
}
