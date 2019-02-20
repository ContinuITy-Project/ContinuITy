package org.continuity.wessbas.managers;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.assertj.core.api.Condition;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.spec.research.open.xtrace.api.core.SubTrace;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.HTTPMethod;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.SubTraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.TraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;

import open.xtrace.OPENxtraceUtils;

public class ModularizationTraceReplacementTest {

	private static final int MILLIS_TO_NANOS = 1000000;

	private Random random;

	private WorkloadModularizationManager manager;
	private Method method;
	private List<HTTPRequestProcessingImpl> callables;
	private Map<String, String> services;
	private Application application;

	@Before
	public void setup() throws NoSuchMethodException, SecurityException {
		random = new Random(3546);

		manager = new WorkloadModularizationManager(null);
		method = WorkloadModularizationManager.class.getDeclaredMethod("getTracesPerState", List.class, Application.class);
		method.setAccessible(true);

		Trace trace1 = generateTrace("carts", 2000, 1000 * MILLIS_TO_NANOS, 2100, 400 * MILLIS_TO_NANOS);
		Trace trace2 = generateTrace("orders", 3000, 1000 * MILLIS_TO_NANOS, 3100, 700 * MILLIS_TO_NANOS);

		callables = new ArrayList<>();
		callables.add((HTTPRequestProcessingImpl) trace1.getRoot().getRoot());
		callables.add((HTTPRequestProcessingImpl) trace2.getRoot().getRoot());

		services = new HashMap<>();
		services.put("carts", "carts");
		services.put("orders", "orders");

		application = createApplication();
	}

	@Test
	public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		@SuppressWarnings("unchecked")
		Map<String, List<Trace>> replacingTraces = (Map<String, List<Trace>>) method.invoke(manager, callables, application);

		assertThat(replacingTraces).containsOnlyKeys("getCarts");
		assertThat(replacingTraces.get("getCarts")).hasSize(2);
		assertThat(replacingTraces.get("getCarts")).extracting(Trace::getRoot).extracting(SubTrace::getRoot)
				.are(new Condition<>(c -> c instanceof HTTPRequestProcessingImpl, "Callable is of type HTTPRequestProcessingImpl"));
		assertThat(replacingTraces.get("getCarts")).extracting(Trace::getRoot).extracting(SubTrace::getRoot).extracting(c -> (HTTPRequestProcessingImpl) c)
				.extracting(HTTPRequestProcessingImpl::getHTTPHeaders).extracting(Optional::get).extracting(m -> m.get("cookie")).extracting(OPENxtraceUtils::extractSessionIdFromCookies)
				.are(new Condition<>(sid -> sid.startsWith("generated"), "Session ID is generated"));
	}

	private Trace generateTrace(String targetService, long timeStampOfRootCallable, long durationOfRootCallable, long timeStampOfModularizedCallable, long durationOfModularizedCallable) {
		TraceImpl trace = new TraceImpl(1);
		SubTraceImpl subtraceFrontEnd = new SubTraceImpl(1, null, trace);
		subtraceFrontEnd.setLocation(new LocationImpl("front-end", 80, "linux", "sock-shop", "getCarts"));
		trace.setRoot(subtraceFrontEnd);
		HTTPRequestProcessingImpl httpRequestProcessingFrontend = new HTTPRequestProcessingImpl(null, subtraceFrontEnd);
		subtraceFrontEnd.setRoot(httpRequestProcessingFrontend);
		httpRequestProcessingFrontend.setUri("/carts");
		httpRequestProcessingFrontend.setTimestamp(timeStampOfRootCallable);
		httpRequestProcessingFrontend.setIdentifier(random.nextLong());
		httpRequestProcessingFrontend.setResponseTime(durationOfRootCallable);
		httpRequestProcessingFrontend.setRequestMethod(HTTPMethod.GET);
		httpRequestProcessingFrontend.setHTTPHeaders(new HashMap<String, String>());
		random.nextBytes(new byte[10]);
		httpRequestProcessingFrontend.getHTTPHeaders().get().put("cookie", "JSESSIONID=" + Long.toString(random.nextLong()));

		RemoteInvocationImpl remoteInvocationTargetsCarts = new RemoteInvocationImpl(httpRequestProcessingFrontend, subtraceFrontEnd);
		httpRequestProcessingFrontend.addCallee(remoteInvocationTargetsCarts);

		SubTraceImpl subTraceCarts = new SubTraceImpl();
		subTraceCarts.setLocation(new LocationImpl(targetService, 80, "linux", "sock-shop", "getCartsSubCall"));

		remoteInvocationTargetsCarts.setTargetSubTrace(subTraceCarts);

		HTTPRequestProcessingImpl httpRequestProcessingCarts = new HTTPRequestProcessingImpl(null, subTraceCarts);
		httpRequestProcessingCarts.setTimestamp(timeStampOfModularizedCallable);
		httpRequestProcessingCarts.setIdentifier(random.nextLong());
		httpRequestProcessingCarts.setResponseTime(durationOfModularizedCallable);
		httpRequestProcessingCarts.setUri("/" + targetService);
		httpRequestProcessingCarts.setRequestMethod(HTTPMethod.GET);
		httpRequestProcessingCarts.setHTTPHeaders(new HashMap<String, String>());
		subTraceCarts.setRoot(httpRequestProcessingCarts);

		return trace;
	}

	private Application createApplication() {
		Application application = new Application();

		HttpEndpoint getCarts = new HttpEndpoint();
		getCarts.setId("getCarts");
		getCarts.setDomain("front-end");
		getCarts.setPort("80");
		getCarts.setMethod("GET");
		getCarts.setPath("/carts");

		application.addEndpoint(getCarts);

		return application;
	}

}
