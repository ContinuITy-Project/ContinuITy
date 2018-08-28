package org.continuity.session.logs.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.pdmodel.MissingResourceException;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.api.core.callables.NestingCallable;
import org.spec.research.open.xtrace.api.core.callables.RemoteInvocation;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.web.client.RestTemplate;

public class ModularizedOPENxtraceSessionLogsExtractor extends OPENxtraceSessionLogsExtractor {

	/**
	 * The hostnames of the services, which have to be tested.
	 */
	private Map<String, String> services;

	/**
	 * Constructor.
	 * 
	 * @param tag                The tag of the application
	 * @param eurekaRestTemplate eureka rest template
	 * @param hostNames          The hostnames of the services, which have to be
	 *                           tested.
	 */
	public ModularizedOPENxtraceSessionLogsExtractor(String tag, RestTemplate eurekaRestTemplate,
			Map<String, String> services) {
		super(tag, eurekaRestTemplate);
		this.services = services;
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
				businessTransactions
						.putAll(getBusinessTransactionsFromApplicationModel(applicationModel, httpCallables));
			}
		}
		return getSessionLogsAsString(sortedList, businessTransactions);
	}

	/**
	 * GetRemoteInvocation
	 */

	/**
	 * {@inheritDoc} Has additional condition: Only returns callables, which target
	 * one of the provided hostnames.
	 */
	@Override
	protected List<HTTPRequestProcessingImpl> diveForHTTPRequestProcessingCallable(Callable callable) {
		List<HTTPRequestProcessingImpl> httpRequestProcessingCallables = new ArrayList<HTTPRequestProcessingImpl>();
		if (null != callable) {
			LinkedBlockingQueue<Callable> callables = new LinkedBlockingQueue<Callable>();
			callables.add(callable);
			String currentCookie = "";
			while (!callables.isEmpty()) {
				Callable currentCallable = callables.poll();
				if (currentCallable instanceof HTTPRequestProcessingImpl) {
					HTTPRequestProcessingImpl httpCallable = (HTTPRequestProcessingImpl) currentCallable;
					// We assume here, that if the port is not provided, the standard port is used
					if (httpCallable.getContainingSubTrace().getLocation().getPort() == -1
							&& httpCallable.getContainingSubTrace().getLocation() instanceof LocationImpl) {
						((LocationImpl) httpCallable.getContainingSubTrace().getLocation()).setPort(80);
					}

					if (httpCallable.getHTTPHeaders().get().containsKey("cookie")) {
						currentCookie = httpCallable.getHTTPHeaders().get().get("cookie");
					}

					try {
						if (callableTargetsDedicatedHost(httpCallable)) {
							httpCallable.getHTTPHeaders().get().put("cookie", currentCookie);
							httpRequestProcessingCallables.add(httpCallable);
						} else {
							callables.addAll(httpCallable.getCallees());
						}
					} catch (MissingResourceException e) {
						e.printStackTrace();
					}
				} else if (currentCallable instanceof NestingCallable) {
					callables.addAll(((NestingCallable) currentCallable).getCallees());
				} else if (currentCallable instanceof RemoteInvocation
						&& ((RemoteInvocation) currentCallable).getTargetSubTrace().isPresent()
						&& null != ((RemoteInvocation) currentCallable).getTargetSubTrace().get().getRoot()) {
					callables.add(((RemoteInvocation) currentCallable).getTargetSubTrace().get().getRoot());
				}
			}
		}
		return httpRequestProcessingCallables;
	}

	/**
	 * Checks, whether the callable targets the dedicated host.
	 * 
	 * @param httpCallable The {@link HTTPRequestProcessingImpl}, which is analyzed.
	 * @return true, if it targets one of the dedicated hosts, else false
	 * @throws MissingResourceException
	 * 
	 */
	private boolean callableTargetsDedicatedHost(HTTPRequestProcessingImpl httpCallable)
			throws MissingResourceException {
		if (!services.values().contains("undefined")) {
			return services.values().contains(httpCallable.getContainingSubTrace().getLocation().getHost());
		} else {
			for (String tag : services.keySet()) {
				Application applicationModel = retrieveApplicationModel(tag);
				if (applicationModel == null) {
					throw new MissingResourceException(
							"No application model found. Please either define an application model or provide a hostname");
				} else {
					RequestUriMapper uriMapper = new RequestUriMapper(applicationModel);
					HttpEndpoint interf = uriMapper.map(httpCallable.getUri(),
							httpCallable.getRequestMethod().get().name());

					if (interf != null && interf.getDomain()
							.equals(httpCallable.getContainingSubTrace().getLocation().getHost())) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
