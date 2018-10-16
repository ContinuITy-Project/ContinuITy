package org.continuity.session.logs.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.continuity.api.entities.artifact.ModularizedSessionLogs;
import org.continuity.api.entities.artifact.ProcessingTimeNormalDistributions;
import org.continuity.api.rest.RestApi.IdpaApplication;
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

import org.junit.Assert;

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
		Mockito.when(restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl("carts-tag").get(), Application.class)).thenReturn(generateSimpleApplicationModel("carts"));
		Mockito.when(restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl("orders-tag").get(), Application.class)).thenReturn(generateSimpleApplicationModel("orders"));

		extractor =  new ModularizedOPENxtraceSessionLogsExtractor("tag",restTemplate , services);
	}
	
	
	/**
	 * Tests the {@link ModularizedOPENxtraceSessionLogsExtractor#getSessionLogsAndThinkTimes(Iterable)}
	 * @author Tobias Angerstein
	 *
	 */
	public static class getSessionLogsAndThinkTimes extends ModularizedOPENxtraceSessionLogsExtractorTest {
		
		
		/**
		 * Tests the generation of {@link ProcessingTimeNormalDistributions} 
		 */
		@Test
		public void testGenerationOfProcessingTimeNormalDistributions() {
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
			
			ModularizedSessionLogs sessionLogs = extractor.getSessionLogsAndThinkTimes(traces);
			
			Map<String, ProcessingTimeNormalDistributions> processingNormalTimeDistributionsMap = sessionLogs.getNormalDistributions();
			
			Assert.assertEquals("Expect the map to have three entries", 3, processingNormalTimeDistributionsMap.size());
			Assert.assertTrue(processingNormalTimeDistributionsMap.containsKey("carts#/carts"));
			Assert.assertTrue(processingNormalTimeDistributionsMap.containsKey("orders#/orders"));
			Assert.assertTrue(processingNormalTimeDistributionsMap.containsKey("root"));
			Assert.assertEquals(133.3333333333333, processingNormalTimeDistributionsMap.get("carts#/carts").getPreprocessingTimeMean(), 0.000001);
			Assert.assertEquals(266.6666666666666, processingNormalTimeDistributionsMap.get("carts#/carts").getPostprocessingTimeMean(), 0.000001);
			Assert.assertEquals(133.3333333333333, processingNormalTimeDistributionsMap.get("orders#/orders").getPreprocessingTimeMean(), 0.000001);
			Assert.assertEquals(266.6666666666666, processingNormalTimeDistributionsMap.get("orders#/orders").getPostprocessingTimeMean(), 0.000001);
			Assert.assertEquals(500, processingNormalTimeDistributionsMap.get("root").getPreprocessingTimeMean(), 0.000001);
			Assert.assertEquals(500, processingNormalTimeDistributionsMap.get("root").getPostprocessingTimeMean(), 0.000001);
			Assert.assertEquals(0, processingNormalTimeDistributionsMap.get("root").getPostprocessingTimeDeviation(), 0.000001);




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
			
			RemoteInvocationImpl remoteInvocationTargetsCarts = new RemoteInvocationImpl(httpRequestProcessingFrontend,subtraceFrontEnd);
			httpRequestProcessingFrontend.addCallee(remoteInvocationTargetsCarts);
			
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
		endpoint.setId(host+"#"+"/"+host);
		endpoint.setDomain(host);
		endpoint.setMethod("GET");
		endpoint.setPath("/"+host);
		endpoint.setPort("80");
		endpoint.setProtocol("HTTP");
		application.addEndpoint(endpoint);
		return application;
	}
}
