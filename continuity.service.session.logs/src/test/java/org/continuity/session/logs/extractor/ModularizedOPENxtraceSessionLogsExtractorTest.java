package org.continuity.session.logs.extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.session.logs.entities.TraceRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.spec.research.open.xtrace.api.core.callables.HTTPMethod;
import org.spec.research.open.xtrace.dflt.impl.core.LocationImpl;
import org.spec.research.open.xtrace.dflt.impl.core.SubTraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.TraceImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Tests the {@link ModularizedOPENxtraceSessionLogsExtractor} class.
 * @author Tobias Angerstein
 *
 */
public class ModularizedOPENxtraceSessionLogsExtractorTest {

	private static final int MILLIS_TO_NANOS = 1000000;

	private List<String> services;

	private VersionOrTimestamp version;

	private RequestTailorer tailorerWithPrePostProcessing;

	private RequestTailorer tailorerWithoutPrePostProcessing;

	private SessionUpdater updater;

	/**
	 * Random number generator
	 */
	protected Random random = new Random();

	/**
	 * Initializer
	 *
	 * @throws ParseException
	 * @throws NumberFormatException
	 */
	@Before
	public void initializeExtractor() throws NumberFormatException, ParseException {
		services = Arrays.asList("carts", "orders");
		RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

		Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(Application[].class)))
				.thenReturn(ResponseEntity.ok(new Application[] { generateSimpleApplicationModel("carts"), generateSimpleApplicationModel("orders") }));

		AppId aid = AppId.fromString("test");
		version = VersionOrTimestamp.fromString("v1");

		tailorerWithPrePostProcessing = new RequestTailorer(aid, version, restTemplate, true);
		tailorerWithoutPrePostProcessing = new RequestTailorer(aid, version, restTemplate, false);
		updater = new SessionUpdater(version, services, Long.MAX_VALUE, true);
	}


	@Test
	public void testWithoutPrePostProcessing() {
		String sessionLogs = generateSessionLogs(tailorerWithoutPrePostProcessing);

		assertThat(sessionLogs.split("\\n")).hasSize(1);
		assertThat(sessionLogs.split(";")).hasSize(7);
	}

	@Test
	public void testWithPrePostProcessing() {
		String sessionLogs = generateSessionLogs(tailorerWithPrePostProcessing);

		assertThat(sessionLogs.split("\\n")).hasSize(1);
		assertThat(sessionLogs.split(";")).hasSize(1 + 6 + 12); // session ID + requests + pre &
																// post processing

		String[] expectedBts = { "\"PRE_PROCESSING#getCarts\"", "\"carts#carts\"", "\"POST_PROCESSING#getCarts\"", "\"PRE_PROCESSING#getCarts\"", "\"carts#carts\"", "\"POST_PROCESSING#getCarts\"",
				"\"PRE_PROCESSING#getCarts\"", "\"carts#carts\"", "\"POST_PROCESSING#getCarts\"", "\"PRE_PROCESSING#getCarts\"", "\"orders#orders\"", "\"POST_PROCESSING#getCarts\"",
				"\"PRE_PROCESSING#getCarts\"", "\"orders#orders\"", "\"POST_PROCESSING#getCarts\"", "\"PRE_PROCESSING#getCarts\"", "\"orders#orders\"", "\"POST_PROCESSING#getCarts\"" };
		assertThat(sessionLogs.split(";")).filteredOn(s -> !s.startsWith("someId")).extracting(s -> s.split(":")[0]).as("business transactions").containsExactly(expectedBts);

		String[] expectedStartTimes = { "0", "100000000", "1000000000", // trace1
				"1000000000", "1200000000", "2000000000", // trace2
				"2000000000", "2100000000", "3000000000", // trace3
				"3000000000", "3100000000", "4000000000", // trace4
				"4000000000", "4200000000", "5000000000", // trace 5
				"5000000000", "5100000000", "6000000000" }; // trace6
		assertThat(sessionLogs.split(";")).filteredOn(s -> !s.startsWith("someId")).extracting(s -> s.split(":")[1]).as("start times").containsExactly(expectedStartTimes);

		String[] expectedEndTimes = { "0", "800000000", "1000000000", // trace1
				"1000000000", "1900000000", "2000000000", // trace2
				"2000000000", "2500000000", "3000000000", // trace3
				"3000000000", "3800000000", "4000000000", // trace4
				"4000000000", "4900000000", "5000000000", // trace 5
				"5000000000", "5500000000", "6000000000" }; // trace6
		assertThat(sessionLogs.split(";")).filteredOn(s -> !s.startsWith("someId")).extracting(s -> s.split(":")[2]).as("end times").containsExactly(expectedEndTimes);
	}

	private String generateSessionLogs(RequestTailorer tailorer) {
		TraceRecord trace1 = generateTrace("carts", 0, 1000*MILLIS_TO_NANOS, 100, 700*MILLIS_TO_NANOS); //preTime: 100 postTime: 200
		TraceRecord trace2 = generateTrace("carts", 1000, 1000*MILLIS_TO_NANOS, 1200, 700*MILLIS_TO_NANOS); //preTime: 200 postTime: 100
		TraceRecord trace3 = generateTrace("carts", 2000, 1000*MILLIS_TO_NANOS, 2100, 400*MILLIS_TO_NANOS); //preTime: 100 postTime: 500

		TraceRecord trace4 = generateTrace("orders", 3000, 1000*MILLIS_TO_NANOS, 3100, 700*MILLIS_TO_NANOS); //preTime: 100 postTime: 200
		TraceRecord trace5 = generateTrace("orders", 4000, 1000*MILLIS_TO_NANOS, 4200, 700*MILLIS_TO_NANOS); //preTime: 200 postTime: 100
		TraceRecord trace6 = generateTrace("orders", 5000, 1000*MILLIS_TO_NANOS, 5100, 400*MILLIS_TO_NANOS); //preTime: 100 postTime: 500

		List<TraceRecord> traces = Arrays.asList(trace1, trace2, trace3, trace4, trace5, trace6);

		List<SessionRequest> requests = tailorer.tailorTraces(services, traces);

		return updater.updateSessions(Collections.emptyList(), requests).stream().map(Session::toExtensiveLog).collect(Collectors.joining("\n"));
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
	private TraceRecord generateTrace(String targetService, long timeStampOfRootCallable, long durationOfRootCallable, long timeStampOfModularizedCallable, long durationOfModularizedCallable) {
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

		return new TraceRecord(version, trace);
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
