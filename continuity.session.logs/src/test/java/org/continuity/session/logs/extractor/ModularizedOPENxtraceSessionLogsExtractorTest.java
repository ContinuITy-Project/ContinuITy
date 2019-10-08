package org.continuity.session.logs.extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.continuity.api.rest.RestApi;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.HTTPMethod;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.SubTraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.TraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;
import org.springframework.web.client.RestTemplate;

/**
 * Tests the {@link ModularizedOPENxtraceSessionLogsExtractor} class.
 * @author Tobias Angerstein
 *
 */
public class ModularizedOPENxtraceSessionLogsExtractorTest {

	private static final int MILLIS_TO_NANOS = 1000000;

	/**
	 * The extractor under test.
	 */
	protected ModularizedOPENxtraceSessionLogsExtractor extractor;
	protected ModularizedOPENxtraceSessionLogsExtractor extractorWithPrePostProcessing;

	/**
	 * Random number generator
	 */
	protected Random random = new Random();

	/**
	 * Initializer
	 */
	@Before
	public void initializeExtractor() {
		HashMap<String, String> services = new HashMap<String, String>();
		services.put("carts-tag", "carts");
		services.put("orders-tag", "orders");
		RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
		Mockito.when(restTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl("carts-tag").get(), Application.class)).thenReturn(generateSimpleApplicationModel("carts"));
		Mockito.when(restTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl("orders-tag").get(), Application.class)).thenReturn(generateSimpleApplicationModel("orders"));

		extractor =  new ModularizedOPENxtraceSessionLogsExtractor("tag",restTemplate , services);
		extractorWithPrePostProcessing = new ModularizedOPENxtraceSessionLogsExtractor("tag",restTemplate , services, true);
	}


	/**
	 * Tests the {@link ModularizedOPENxtraceSessionLogsExtractor#getSessionLogsAndThinkTimes(Iterable)}
	 * @author Tobias Angerstein
	 *
	 */
	public static class getSessionLogsAndThinkTimes extends ModularizedOPENxtraceSessionLogsExtractorTest {

		@Test
		public void testWithoutPrePostProcessing() {
			String sessionLogs = generateSessionLogs(extractor);

			assertThat(sessionLogs.split("\\n")).hasSize(1);
			assertThat(sessionLogs.split(";")).hasSize(7);
		}

		@Test
		public void testWithPrePostProcessing() {
			String sessionLogs = generateSessionLogs(extractorWithPrePostProcessing);

			assertThat(sessionLogs.split("\\n")).hasSize(1);
			assertThat(sessionLogs.split(";")).hasSize(1 + 6 + 12); // session ID + requests + pre &
																	// post processing

			String[] expectedBts = { "\"PRE_PROCESSING#getCarts\"", "\"carts#carts\"", "\"POST_PROCESSING#getCarts\"", "\"PRE_PROCESSING#getCarts\"", "\"carts#carts\"", "\"POST_PROCESSING#getCarts\"",
					"\"PRE_PROCESSING#getCarts\"", "\"carts#carts\"", "\"POST_PROCESSING#getCarts\"", "\"PRE_PROCESSING#getCarts\"", "\"orders#orders\"", "\"POST_PROCESSING#getCarts\"",
					"\"PRE_PROCESSING#getCarts\"", "\"orders#orders\"", "\"POST_PROCESSING#getCarts\"", "\"PRE_PROCESSING#getCarts\"", "\"orders#orders\"", "\"POST_PROCESSING#getCarts\"" };
			assertThat(sessionLogs.split(";")).filteredOn(s -> !"someId".equals(s)).extracting(s -> s.split(":")[0]).as("business transactions").containsExactly(expectedBts);

			String[] expectedStartTimes = { "0", "100000000", "1000000000", // trace1
					"1000000000", "1200000000", "2000000000", // trace2
					"2000000000", "2100000000", "3000000000", // trace3
					"3000000000", "3100000000", "4000000000", // trace4
					"4000000000", "4200000000", "5000000000", // trace 5
					"5000000000", "5100000000", "6000000000" }; // trace6
			assertThat(sessionLogs.split(";")).filteredOn(s -> !"someId".equals(s)).extracting(s -> s.split(":")[1]).as("start times").containsExactly(expectedStartTimes);

			String[] expectedEndTimes = { "0", "800000000", "1000000000", // trace1
					"1000000000", "1900000000", "2000000000", // trace2
					"2000000000", "2500000000", "3000000000", // trace3
					"3000000000", "3800000000", "4000000000", // trace4
					"4000000000", "4900000000", "5000000000", // trace 5
					"5000000000", "5500000000", "6000000000" }; // trace6
			assertThat(sessionLogs.split(";")).filteredOn(s -> !"someId".equals(s)).extracting(s -> s.split(":")[2]).as("end times").containsExactly(expectedEndTimes);
		}

		private String generateSessionLogs(ModularizedOPENxtraceSessionLogsExtractor extractor) {
			Trace trace1 = generateTrace("carts", 0, 1000*MILLIS_TO_NANOS, 100, 700*MILLIS_TO_NANOS); //preTime: 100 postTime: 200
			Trace trace2 = generateTrace("carts", 1000, 1000*MILLIS_TO_NANOS, 1200, 700*MILLIS_TO_NANOS); //preTime: 200 postTime: 100
			Trace trace3 = generateTrace("carts", 2000, 1000*MILLIS_TO_NANOS, 2100, 400*MILLIS_TO_NANOS); //preTime: 100 postTime: 500

			Trace trace4 = generateTrace("orders", 3000, 1000*MILLIS_TO_NANOS, 3100, 700*MILLIS_TO_NANOS); //preTime: 100 postTime: 200
			Trace trace5 = generateTrace("orders", 4000, 1000*MILLIS_TO_NANOS, 4200, 700*MILLIS_TO_NANOS); //preTime: 200 postTime: 100
			Trace trace6 = generateTrace("orders", 5000, 1000*MILLIS_TO_NANOS, 5100, 400*MILLIS_TO_NANOS); //preTime: 100 postTime: 500

			Trace trace7 = generateTrace("user", 0, 1000*MILLIS_TO_NANOS, 100, 700*MILLIS_TO_NANOS); //preTime: 500 postTime: 500
			Trace trace8 = generateTrace("user", 1000, 1000*MILLIS_TO_NANOS, 1200, 700*MILLIS_TO_NANOS); //preTime: 500 postTime: 500
			Trace trace9 = generateTrace("user", 2000, 1000*MILLIS_TO_NANOS, 2100, 400*MILLIS_TO_NANOS); //preTime: 500 postTime: 500

			Iterable<Trace> traces = new ArrayList<Trace>(Arrays.asList(trace1, trace2, trace3, trace4, trace5, trace6, trace7, trace8, trace9));

			return extractor.getSessionLogs(traces);
		}

		/**
		 * Generates Traces
		 * @param targetService
		 * @param timeStampOfRootCallable
		 * @param durationOfRootCallable
		 * @param timeStampOfModularizedCallable
		 * @param durationOfModularizedCallable
		 * @return
		 */
		private Trace generateTrace(String targetService, long timeStampOfRootCallable, long durationOfRootCallable, long timeStampOfModularizedCallable, long durationOfModularizedCallable) {
			TraceImpl trace = new TraceImpl(1);
			SubTraceImpl subtraceFrontEnd = new SubTraceImpl(1,null, trace);
			subtraceFrontEnd.setLocation(new LocationImpl("front-end", 80, "linux", "sock-shop", "getCarts"));
			trace.setRoot(subtraceFrontEnd);
			HTTPRequestProcessingImpl httpRequestProcessingFrontend = new HTTPRequestProcessingImpl(null, subtraceFrontEnd);
			subtraceFrontEnd.setRoot(httpRequestProcessingFrontend);
			httpRequestProcessingFrontend.setTimestamp(timeStampOfRootCallable);
			httpRequestProcessingFrontend.setIdentifier(random.nextLong());
			httpRequestProcessingFrontend.setResponseTime(durationOfRootCallable);
			httpRequestProcessingFrontend.setRequestMethod(HTTPMethod.GET);
			httpRequestProcessingFrontend.setHTTPHeaders(new HashMap<String, String>());
			httpRequestProcessingFrontend.getHTTPHeaders().get().put("cookie", "JSESSIONID=someId");
			httpRequestProcessingFrontend.setResponseCode(200);

			RemoteInvocationImpl remoteInvocationTargetsCarts = new RemoteInvocationImpl(httpRequestProcessingFrontend,subtraceFrontEnd);

			SubTraceImpl subTraceCarts = new SubTraceImpl();
			subTraceCarts.setLocation(new LocationImpl(targetService, 80, "linux", "sock-shop", "getCarts"));

			remoteInvocationTargetsCarts.setTargetSubTrace(subTraceCarts);

			HTTPRequestProcessingImpl httpRequestProcessingCarts = new HTTPRequestProcessingImpl(null, subTraceCarts);
			httpRequestProcessingCarts.setTimestamp(timeStampOfModularizedCallable);
			httpRequestProcessingCarts.setIdentifier(random.nextLong());
			httpRequestProcessingCarts.setResponseTime(durationOfModularizedCallable);
			httpRequestProcessingCarts.setUri("/"+targetService);
			httpRequestProcessingCarts.setRequestMethod(HTTPMethod.GET);
			httpRequestProcessingCarts.setHTTPHeaders(new HashMap<String, String>());
			httpRequestProcessingCarts.setResponseCode(200);
			subTraceCarts.setRoot(httpRequestProcessingCarts);

			return trace;
		}
	}

	/**
	 * Generates simple application
	 * @param host
	 * the target host
	 * @param uri
	 * the uri/ path
	 * @return
	 *  An application model containing one {@link HttpEndpoint}
	 */
	private Application generateSimpleApplicationModel(String host) {
		Application application = new Application();
		HttpEndpoint endpoint  = new HttpEndpoint();
		endpoint.setId(host + "#" + host);
		endpoint.setDomain(host);
		endpoint.setMethod("GET");
		endpoint.setPath("/"+host);
		endpoint.setPort("80");
		endpoint.setProtocol("HTTP");
		application.addEndpoint(endpoint);
		return application;
	}
}
