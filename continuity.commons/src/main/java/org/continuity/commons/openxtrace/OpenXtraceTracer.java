package org.continuity.commons.openxtrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.api.core.callables.NestingCallable;
import org.spec.research.open.xtrace.api.core.callables.RemoteInvocation;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;

/**
 * Allows to trace through a OPEN.xtrace, potentially considering a set of hosts of interest.
 *
 * @author Henning Schulz, Tobias Angerstein
 *
 */
public class OpenXtraceTracer {

	private final Callable rootCallable;

	private final Collection<String> hostsOfInterest;

	private List<HTTPRequestProcessingImpl> extracted;

	private OpenXtraceTracer(Callable rootCallable, Collection<String> hostsOfInterest) {
		this.rootCallable = rootCallable;
		this.hostsOfInterest = hostsOfInterest;
	}

	private OpenXtraceTracer(Callable rootCallable) {
		this(rootCallable, null);
	}

	/**
	 * Creates a tracer for the given root {@link Callable}.
	 *
	 * @param rootCallable
	 *            The root callable from which on the tracing will start.
	 * @return The tracer.
	 */
	public static OpenXtraceTracer forRoot(Callable rootCallable) {
		return new OpenXtraceTracer(rootCallable);
	}

	/**
	 * Creates a tracer for the given root {@link Callable} and a list of host names to be
	 * considered.
	 *
	 * @param rootCallable
	 *            The root callable from which on the tracing will start.
	 * @param hostsOfInteres
	 *            The host names to be considered when tracing through the OPEN.xtrace.
	 * @return The tracer.
	 */
	public static OpenXtraceTracer forRootAndHosts(Callable rootCallable, String... hostsOfInterest) {
		return new OpenXtraceTracer(rootCallable, Arrays.asList(hostsOfInterest));
	}

	/**
	 * Creates a tracer for the given root {@link Callable} and a list of host names to be
	 * considered.
	 *
	 * @param rootCallable
	 *            The root callable from which on the tracing will start.
	 * @param hostsOfInteres
	 *            The host names to be considered when tracing through the OPEN.xtrace.
	 * @return The tracer.
	 */
	public static OpenXtraceTracer forRootAndHosts(Callable rootCallable, Collection<String> hostsOfInterest) {
		return new OpenXtraceTracer(rootCallable, hostsOfInterest);
	}

	/**
	 * Returns the {@link HTTPRequestProcessingImpl} objects of the corresponding
	 * {@link RemoteInvocationImpl} which are on the highest level. If a list of hosts of interest
	 * has been defined, only those requests targeting these hosts are considered.
	 *
	 * @return List of {@link HTTPRequestProcessingImpl}
	 */
	public List<HTTPRequestProcessingImpl> extractSubtraces() {
		if (extracted != null) {
			return extracted;
		}

		if (hostsOfInterest == null) {
			extracted = extractSubtracesIgnoringHosts();
		} else {
			extracted = extractSubtracesForHosts();
		}

		return extracted;
	}

	/**
	 * Returns the {@link HTTPRequestProcessingImpl} objects of the corresponding
	 * {@link RemoteInvocationImpl} which are on the highest level.
	 *
	 * @return List of {@link HTTPRequestProcessingImpl}
	 */
	private List<HTTPRequestProcessingImpl> extractSubtracesIgnoringHosts() {
		List<HTTPRequestProcessingImpl> httpRequestProcessingCallables = new ArrayList<HTTPRequestProcessingImpl>();
		if (null != rootCallable) {
			LinkedBlockingQueue<Callable> callables = new LinkedBlockingQueue<Callable>();
			callables.add(rootCallable);
			while (!callables.isEmpty()) {
				Callable currentCallable = callables.poll();
				if (currentCallable instanceof HTTPRequestProcessingImpl) {
					httpRequestProcessingCallables.add((HTTPRequestProcessingImpl) currentCallable);
				} else if ((currentCallable instanceof NestingCallable) && httpRequestProcessingCallables.isEmpty()) {
					callables.addAll(((NestingCallable) currentCallable).getCallees());
				} else if ((currentCallable instanceof RemoteInvocationImpl) && ((RemoteInvocationImpl) currentCallable).getTargetSubTrace().isPresent()
						&& (null != ((RemoteInvocationImpl) currentCallable).getTargetSubTrace().get().getRoot())) {
					callables.add(((RemoteInvocationImpl) currentCallable).getTargetSubTrace().get().getRoot());
				}
			}
		}
		return httpRequestProcessingCallables;
	}

	/**
	 * Returns the {@link HTTPRequestProcessingImpl} objects of the corresponding
	 * {@link RemoteInvocationImpl} which are on the highest level and target one of the hosts of
	 * interest. Furthermore, it sets the cookie header of the original root callable to the
	 * determined child requests.
	 *
	 * @return List of {@link HTTPRequestProcessingImpl}
	 */
	private List<HTTPRequestProcessingImpl> extractSubtracesForHosts() {
		List<HTTPRequestProcessingImpl> httpRequestProcessingCallables = new ArrayList<HTTPRequestProcessingImpl>();
		if (null != rootCallable) {
			LinkedBlockingQueue<Callable> callables = new LinkedBlockingQueue<Callable>();
			callables.add(rootCallable);
			String currentCookie = "";
			while (!callables.isEmpty()) {
				Callable currentCallable = callables.poll();
				if (currentCallable instanceof HTTPRequestProcessingImpl) {
					HTTPRequestProcessingImpl httpCallable = (HTTPRequestProcessingImpl) currentCallable;
					// We assume here, that if the port is not provided, the standard port is used
					if ((httpCallable.getContainingSubTrace().getLocation().getPort() == -1) && (httpCallable.getContainingSubTrace().getLocation() instanceof LocationImpl)) {
						((LocationImpl) httpCallable.getContainingSubTrace().getLocation()).setPort(80);
					}

					if (httpCallable.getHTTPHeaders().get().containsKey("cookie")) {
						currentCookie = httpCallable.getHTTPHeaders().get().get("cookie");
					}

					if (hostsOfInterest.contains(httpCallable.getContainingSubTrace().getLocation().getHost())) {
						httpCallable.getHTTPHeaders().get().put("cookie", currentCookie);
						httpRequestProcessingCallables.add(httpCallable);
					} else {
						callables.addAll(httpCallable.getCallees());
					}
				} else if (currentCallable instanceof NestingCallable) {
					callables.addAll(((NestingCallable) currentCallable).getCallees());
				} else if ((currentCallable instanceof RemoteInvocation) && ((RemoteInvocation) currentCallable).getTargetSubTrace().isPresent()
						&& (null != ((RemoteInvocation) currentCallable).getTargetSubTrace().get().getRoot())) {
					callables.add(((RemoteInvocation) currentCallable).getTargetSubTrace().get().getRoot());
				}
			}
		}
		return httpRequestProcessingCallables;
	}

}
