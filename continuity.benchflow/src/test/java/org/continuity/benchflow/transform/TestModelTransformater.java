package org.continuity.benchflow.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;
import org.continuity.benchflow.BenchFlowTestHelper;
import org.continuity.benchflow.artifact.ContinuITyModel;
import org.continuity.benchflow.artifact.ThinkTimeHelper;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.JsonPathExtraction;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.annotation.ValueExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import cloud.benchflow.dsl.definition.types.percent.Percent;
import cloud.benchflow.dsl.definition.workload.HttpWorkload;
import cloud.benchflow.dsl.definition.workload.drivertype.DriverType;
import cloud.benchflow.dsl.definition.workload.interoperationtimingstype.InterOperationsTimingType;
import cloud.benchflow.dsl.definition.workload.mix.MatrixMix;
import cloud.benchflow.dsl.definition.workload.mix.Mix;
import cloud.benchflow.dsl.definition.workload.operation.Operation;
import cloud.benchflow.dsl.definition.workload.operation.body.Body;
import cloud.benchflow.dsl.definition.workload.operation.body.BodyForm;
import cloud.benchflow.dsl.definition.workload.operation.extraction.Extraction;
import cloud.benchflow.dsl.definition.workload.operation.method.Method;
import cloud.benchflow.dsl.definition.workload.operation.parameter.Parameter;
import cloud.benchflow.dsl.definition.workload.operation.protocol.Protocol;
import cloud.benchflow.dsl.definition.workload.workloaditem.HttpWorkloadItem;
import scala.collection.JavaConverters;
import scala.collection.Seq;

public class TestModelTransformater {

	private ModelTransformater transformater;
	
	@Before
	public void setUp() {
		transformater = new ModelTransformater();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyContinuITyModel() {
		transformater.transformToBenchFlow(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyModels() {
		transformater.transformToBenchFlow(new ContinuITyModel(null, null, null));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetBehaviorWithoutBehaviors() throws Exception {
		Behavior behavior = new Behavior();
		ThinkTimeHelper thinkTimeHelper = new ThinkTimeHelper();
		Whitebox.invokeMethod(transformater, "getBehavior", behavior, thinkTimeHelper);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetBehaviorWithMissingInitialState() throws Exception {

		Behavior behavior = new Behavior();
		behavior.setName("test_behavior");
		behavior.setProbability(1.0);
		
		List<MarkovState> markovStates = new ArrayList<MarkovState>();
		MarkovState state = new MarkovState();
		state.setId("state_1");
		markovStates.add(state);	
		behavior.setMarkovStates(markovStates);

		ThinkTimeHelper thinkTimeHelper = new ThinkTimeHelper();
		
		Whitebox.invokeMethod(transformater, "getBehavior", behavior, thinkTimeHelper);
	}
	
	@Test
	public void testGetBehaviorWithOneMarkovState() throws Exception {

		Behavior behavior = new Behavior();
		behavior.setName("test_behavior");
		behavior.setProbability(1.0);
		behavior.setInitialState("state_1");
		
		List<MarkovState> markovStates = new ArrayList<MarkovState>();
		MarkovState state = new MarkovState();
		state.setId("state_1");
		markovStates.add(state);
		behavior.setMarkovStates(markovStates);

		ThinkTimeHelper thinkTimeHelper = new ThinkTimeHelper();
		
		Mix result = Whitebox.invokeMethod(transformater, "getBehavior", behavior, thinkTimeHelper);
		MatrixMix mix = (MatrixMix) result.mix();
		assertEquals(1, mix.mix().size());
		
		Collection<Seq<Percent>> colMix = JavaConverters.asJavaCollection(mix.mix());
		List<Seq<Percent>> listMix = new ArrayList<>(colMix);
		
		assertEquals(1, listMix.get(0).size());
		assertEquals("[Stream(0.0%)]", listMix.toString());
	}
	
	@Test
	public void testGetBehaviorWithTwoMarkovStates() throws Exception {

		Behavior behavior = new Behavior();
		behavior.setName("test_behavior");
		behavior.setProbability(1.0);
		behavior.setInitialState("state_2");
		
		List<MarkovState> markovStates = new ArrayList<MarkovState>();
		
		MarkovState state = new MarkovState();
		state.setId("state_1");
		markovStates.add(state);
		
		MarkovState state2 = new MarkovState();
		state2.setId("state_2");
		markovStates.add(state2);
		
		List<Transition> transitions = new ArrayList<Transition>();
		Transition transition1 = new Transition();
		transition1.setProbability(0.7);
		transition1.setTargetState("state_2");	
		transitions.add(transition1);
		
		Transition transition2 = new Transition();
		transition2.setProbability(0.3);
		transition2.setTargetState("state_1");	
		transitions.add(transition2);
		state2.setTransitions(transitions);
		
		behavior.setMarkovStates(markovStates);

		ThinkTimeHelper thinkTimeHelper = new ThinkTimeHelper();
		
		Mix result = Whitebox.invokeMethod(transformater, "getBehavior", behavior, thinkTimeHelper);
		MatrixMix mix = (MatrixMix) result.mix();
		assertEquals(2, mix.mix().size());
		
		Collection<Seq<Percent>> colMix = JavaConverters.asJavaCollection(mix.mix());
		List<Seq<Percent>> listMix = new ArrayList<>(colMix);
		
		assertEquals(2, listMix.get(0).size());
		assertEquals(2, listMix.get(1).size());
		assertEquals("[Stream(70.0%, 30.0%), Stream(0.0%, 0.0%)]", listMix.toString());
	}
	
	@Test
	public void testGetBehaviorWithFourMarkovStates() throws Exception {

		Behavior behavior = new Behavior();
		behavior.setName("test_behavior");
		behavior.setProbability(1.0);
		behavior.setInitialState("state_1");
		
		List<MarkovState> markovStates = new ArrayList<MarkovState>();
		
		MarkovState state = new MarkovState();
		state.setId("state_1");
		markovStates.add(state);
		
		MarkovState state2 = new MarkovState();
		state2.setId("state_2");
		markovStates.add(state2);
		
		MarkovState state3 = new MarkovState();
		state3.setId("state_3");
		markovStates.add(state3);
		
		MarkovState state4 = new MarkovState();
		state4.setId("state_4");
		markovStates.add(state4);
		
		List<Transition> transitionsS1 = new ArrayList<Transition>();
		Transition transitionS1 = new Transition();
		transitionS1.setProbability(0.3);
		transitionS1.setTargetState("state_2");	
		transitionsS1.add(transitionS1);
		
		Transition transitionS2 = new Transition();
		transitionS2.setProbability(0.7);
		transitionS2.setTargetState("state_3");	
		transitionsS1.add(transitionS2);
		state.setTransitions(transitionsS1);
		
		List<Transition> transitionsS3 = new ArrayList<Transition>();
		Transition transitionS3 = new Transition();
		transitionS3.setProbability(1.0);
		transitionS3.setTargetState("state_4");	
		transitionsS3.add(transitionS3);
		state3.setTransitions(transitionsS3);
		
		behavior.setMarkovStates(markovStates);

		ThinkTimeHelper thinkTimeHelper = new ThinkTimeHelper();
		
		Mix result = Whitebox.invokeMethod(transformater, "getBehavior", behavior, thinkTimeHelper);
		MatrixMix mix = (MatrixMix) result.mix();
		assertEquals(4, mix.mix().size());
		
		Collection<Seq<Percent>> colMix = JavaConverters.asJavaCollection(mix.mix());
		List<Seq<Percent>> listMix = new ArrayList<>(colMix);
		
		assertEquals(4, listMix.get(0).size());
		assertEquals(4, listMix.get(1).size());
		assertEquals(4, listMix.get(2).size());
		assertEquals(4, listMix.get(3).size());
		
		String expected = "[Stream(0.0%, 30.0%, 70.0%, 0.0%), Stream(0.0%, 0.0%, 0.0%, 0.0%), Stream(0.0%, 0.0%, 0.0%, 100.0%), Stream(0.0%, 0.0%, 0.0%, 0.0%)]";
		assertEquals(expected, listMix.toString());
	}

	@Test
	public void testGetExtractionsWithoutExtractedInputs() throws Exception {

		Application application = null;
		List<ExtractedInput> listExtractedInput = new ArrayList<ExtractedInput>();
		String endpointId = "";
		
		Map<String, Extraction> extractions = Whitebox.invokeMethod(transformater, "getExtractions", application, listExtractedInput, endpointId, RegExExtraction.class);
		assertNotNull(extractions);
		assertTrue(extractions.isEmpty());
	}
		
	@Test(expected = IllegalArgumentException.class)
	public void testGetExtractionsWithoutExistingEndpoint() throws Exception {

		Application application = new Application();
		
		HttpEndpoint notExistingEndpoint = new HttpEndpoint();
		notExistingEndpoint.setId("http_endpoint");
		
		List<ExtractedInput> listExtractedInput = new ArrayList<ExtractedInput>();		
		ExtractedInput extractedInput = new ExtractedInput();
		listExtractedInput.add(extractedInput);
		
		List<ValueExtraction> listValueExtractions = new ArrayList<ValueExtraction>();
		RegExExtraction extraction = createRegExExtraction(notExistingEndpoint, "(.*)", null, 0);	
		listValueExtractions.add(extraction);
		
		extractedInput.setExtractions(listValueExtractions);	
		
		String endpointId = "http_endpoint";
		
		Whitebox.invokeMethod(transformater, "getExtractions", application, listExtractedInput, endpointId, RegExExtraction.class);
	}
	
	@Test
	public void testGetExtractionsWithoutMatchingEndpointId() throws Exception {

		Application application = new Application();
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setId("http_endpoint");
		application.addEndpoint(endpoint);
		
		List<ExtractedInput> listExtractedInput = new ArrayList<ExtractedInput>();		
		ExtractedInput extractedInput = new ExtractedInput();
		extractedInput.setId("Input_extraction_all");
		listExtractedInput.add(extractedInput);
		
		List<ValueExtraction> listValueExtractions = new ArrayList<ValueExtraction>();
		RegExExtraction extraction = createRegExExtraction(endpoint, "(.*)", "NOT_FOUND", 0);
		listValueExtractions.add(extraction);
		
		extractedInput.setExtractions(listValueExtractions);	
		
		String endpointId = "http_endpoint_2";
		
		Map<String, Extraction> extractions = Whitebox.invokeMethod(transformater, "getExtractions", application, listExtractedInput, endpointId, RegExExtraction.class);
		assertNotNull(extractions);
		assertTrue(extractions.isEmpty());
	}	
	
	@Test
	public void testGetExtractionsWithRegExExtractionAndMatchedEndpointId() throws Exception {

		Application application = new Application();
		
		// Create endpoint
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setId("http_endpoint");
		application.addEndpoint(endpoint);
		
		// Create inputs
		List<ExtractedInput> listExtractedInput = new ArrayList<ExtractedInput>();		
		ExtractedInput extractedInput = new ExtractedInput();
		extractedInput.setId("Input_extraction_all");
		listExtractedInput.add(extractedInput);
		
		ExtractedInput extractedInput2 = new ExtractedInput();
		extractedInput2.setId("Input_extraction_test");
		listExtractedInput.add(extractedInput2);
		
		// Create extraction values for input 1
		List<ValueExtraction> listValueExtractions = new ArrayList<ValueExtraction>();
		RegExExtraction regExExtraction = createRegExExtraction(endpoint, "(.*)", "NOT_FOUND", 0);
		listValueExtractions.add(regExExtraction);
		
		JsonPathExtraction jsonExtraction = new JsonPathExtraction();
		jsonExtraction.setMatchNumber(0);
		jsonExtraction.setFallbackValue("NOT_FOUND");
		jsonExtraction.setJsonPath("$.root.element[0].timestamp");
		jsonExtraction.setFrom(WeakReference.create(endpoint));
		listValueExtractions.add(regExExtraction);
		
		extractedInput.setExtractions(listValueExtractions);	
		
		// Create extraction values for input 2
		List<ValueExtraction> listValueExtractions2 = new ArrayList<ValueExtraction>();
		
		RegExExtraction regExExtraction2 = createRegExExtraction(endpoint, "id='test' value='(.*)'", null, -1);
		listValueExtractions2.add(regExExtraction2);
		
		extractedInput2.setExtractions(listValueExtractions2);	
		
		String endpointId = "http_endpoint";
		
		// Check test results
		Map<String, Extraction> extractions = Whitebox.invokeMethod(transformater, "getExtractions", application, listExtractedInput, endpointId, RegExExtraction.class);
		assertNotNull(extractions);
		assertEquals(2, extractions.size());
		assertTrue(extractions.containsKey("Input_extraction_all"));
		assertTrue(extractions.containsKey("Input_extraction_test"));
		
		Extraction benchFlowExtraction = extractions.get("Input_extraction_all");
		assertEquals(regExExtraction.getPattern(), benchFlowExtraction.pattern());
		assertEquals(regExExtraction.getFallbackValue(), benchFlowExtraction.fallbackValue().get());
		assertEquals(regExExtraction.getMatchNumber(), benchFlowExtraction.matchNumber().get());
		
		Extraction benchFlowExtraction2 = extractions.get("Input_extraction_test");
		assertEquals(regExExtraction2.getPattern(), benchFlowExtraction2.pattern());
		assertEquals("NOT FOUND", benchFlowExtraction2.fallbackValue().get());
		assertEquals(1, benchFlowExtraction2.matchNumber().get());
	}	
	
	private RegExExtraction createRegExExtraction(HttpEndpoint endpoint, String pattern, String fallbackValue, int matchNumber) {
		RegExExtraction regExExtraction = new RegExExtraction();
		regExExtraction.setPattern(pattern);
		regExExtraction.setFrom(WeakReference.create(endpoint));
		if(matchNumber != -1) {
			regExExtraction.setMatchNumber(matchNumber);			
		}
		if(fallbackValue != null) {
			regExExtraction.setFallbackValue(fallbackValue);			
		}
		
		return regExExtraction;
	}
	
	private void testBehavior1(HttpWorkloadItem workloadItem) {
		// Check Operations
		List<Operation> operations = getJavaListOperation(workloadItem.operations().get());
		assertEquals(3, operations.size());
		
		assertEquals("startUsingOPTIONS", operations.get(0).id());
		assertEquals("productUsingPOST", operations.get(1).id());
		assertEquals("logoutUsingGET", operations.get(2).id());
		
		for(Operation operation : operations) {
			assertFalse(operation.body().isDefined());
			assertFalse(operation.jsonExtraction().isDefined());
			assertFalse(operation.regexExtraction().isDefined());
			assertFalse(operation.headers().isDefined());
			assertFalse(operation.urlParameter().isDefined());
			assertFalse(operation.queryParameter().isDefined());
			assertEquals(Protocol.HTTP, operation.protocol());
		}
		
		Operation operationStart = operations.get(0);
		Operation operationProduct = operations.get(1);
		Operation operationLogout = operations.get(2);
		
		assertFalse(operationStart.thinkTime().isDefined());
		assertFalse(operationProduct.thinkTime().isDefined());
		assertTrue(operationLogout.thinkTime().isDefined());
		
		assertEquals(Method.OPTIONS, operationStart.method());
		assertEquals(Method.POST, operationProduct.method());
		assertEquals(Method.GET, operationLogout.method());
		
		assertEquals("/index.html/start", operationStart.endpoint());
		assertEquals("/product", operationProduct.endpoint());
		assertEquals("/logout", operationLogout.endpoint());
		
		final double[][] PROBABILITY  = 
		{{0.0, 0.7, 0.3},
		{0.0, 0.0, 0.0},
		{0.0, 0.0, 0.0}};
				
		this.checkMix(workloadItem, PROBABILITY);		
	}
	
	private void testBehavior2(HttpWorkloadItem workloadItem) {
		// Check Operations
		List<Operation> operations = getJavaListOperation(workloadItem.operations().get());
		assertEquals(2, operations.size());
		
		assertEquals("productUsingPOST", operations.get(0).id());
		assertEquals("searchUsingPOST", operations.get(1).id());
		
		for(Operation operation : operations) {
			assertFalse(operation.jsonExtraction().isDefined());
			assertFalse(operation.regexExtraction().isDefined());
			assertFalse(operation.headers().isDefined());
			assertFalse(operation.urlParameter().isDefined());
			assertFalse(operation.queryParameter().isDefined());
			assertEquals(Protocol.HTTP, operation.protocol());
		}
		
		Operation operationProduct = operations.get(0);
		Operation operationSearch = operations.get(1);
		
		assertFalse(operationProduct.thinkTime().isDefined());
		assertFalse(operationSearch.thinkTime().isDefined());
		
		assertEquals(Method.POST, operationProduct.method());
		assertEquals(Method.POST, operationSearch.method());
		
		assertEquals("/product", operationProduct.endpoint());
		assertEquals("/search", operationSearch.endpoint());
		
		assertFalse(operationProduct.body().isDefined());
		assertTrue(operationSearch.body().isDefined());
		
		// Check body parameter
		assertTrue(operationSearch.body().get() instanceof BodyForm);
		
		BodyForm bodyForm = (BodyForm)operationSearch.body().get();
		Map<String, Parameter> mapSearchParameter = JavaConverters.mapAsJavaMap(bodyForm.body());
		assertEquals(2, mapSearchParameter.size());
		assertTrue(mapSearchParameter.containsKey("item"));
		assertTrue(mapSearchParameter.containsKey("color"));
		
		assertFalse(mapSearchParameter.get("item").retrieval().isDefined());
		assertFalse(mapSearchParameter.get("color").retrieval().isDefined());
		
		List<String> parameterItem = JavaConverters.seqAsJavaList(mapSearchParameter.get("item").items());
		List<String> parameterColor = JavaConverters.seqAsJavaList(mapSearchParameter.get("color").items());
		assertEquals(1, parameterItem.size());	
		assertEquals(3, parameterColor.size());	
		
		assertEquals("42", parameterItem.get(0));
		assertEquals("black", parameterColor.get(0));
		assertEquals("red", parameterColor.get(1));
		assertEquals("blue", parameterColor.get(2));
		
		final double[][] PROBABILITY  = 
		{{0.0, 0.9},
		{0.0, 0.0}};
				
		this.checkMix(workloadItem, PROBABILITY);
		
	}
	
	@Test
	public void testTransformToBenchFlowWithOneBehaviorAndNoParameter() {
		
		// Init test
		BehaviorModelGenerator behaviorModelGenerator = new BehaviorModelGenerator();
		ContinuITyIDPAGenerator idpaGenerator = new ContinuITyIDPAGenerator();
		
		BehaviorModel behaviorModel = behaviorModelGenerator.createBehaviorModel(1);
		Application application = idpaGenerator.setupApplication();
		ApplicationAnnotation annotation = idpaGenerator.setupAnnotation(application);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, application, annotation);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(1, mapWorkloadItems.size());
		assertTrue(mapWorkloadItems.containsKey("Behavior_WithoutParameter"));

		HttpWorkloadItem workloadItem = mapWorkloadItems.get("Behavior_WithoutParameter");
		assertEquals(DriverType.HTTP, workloadItem.driverType());
		assertFalse(workloadItem.dataSources().nonEmpty());
		assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
		assertFalse(workloadItem.popularity().isDefined());

		testBehavior1(workloadItem);
	}
	
	@Test
	public void testTransformToBenchFlowWithTwoBehaviorAndFormParameter() {
		
		// Init test
		BehaviorModelGenerator behaviorModelGenerator = new BehaviorModelGenerator();
		ContinuITyIDPAGenerator idpaGenerator = new ContinuITyIDPAGenerator();
		
		BehaviorModel behaviorModel = behaviorModelGenerator.createBehaviorModel(2);
		Application application = idpaGenerator.setupApplication();
		ApplicationAnnotation annotation = idpaGenerator.setupAnnotation(application);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, application, annotation);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(2, mapWorkloadItems.size());
		assertTrue(mapWorkloadItems.containsKey("Behavior_WithoutParameter"));
		assertTrue(mapWorkloadItems.containsKey("Behavior_FormParameter"));

		for(HttpWorkloadItem workloadItem : mapWorkloadItems.values()) {
			assertEquals(DriverType.HTTP, workloadItem.driverType());
			assertFalse(workloadItem.dataSources().nonEmpty());
			assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
		}
		
		HttpWorkloadItem workloadItemBehavior0 = mapWorkloadItems.get("Behavior_WithoutParameter");
		HttpWorkloadItem workloadItemBehavior1 = mapWorkloadItems.get("Behavior_FormParameter");
		
		assertEquals(0.2, workloadItemBehavior0.popularity().get().underlying(), 0.000001);
		assertEquals(0.8, workloadItemBehavior1.popularity().get().underlying(), 0.000001);

		testBehavior1(workloadItemBehavior0);
		testBehavior2(workloadItemBehavior1);
	}
	
	@Test
	public void testTransformToBenchFlowWithDifferentParameters() {
		
		// Init test
		BehaviorModelGenerator behaviorModelGenerator = new BehaviorModelGenerator();
		ContinuITyIDPAGenerator idpaGenerator = new ContinuITyIDPAGenerator();
		
		BehaviorModel behaviorModel = behaviorModelGenerator.createBehaviorModel(3);
		Application application = idpaGenerator.setupApplication();

		ApplicationAnnotation annotation = idpaGenerator.setupAnnotation(application);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, application, annotation);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(1, mapWorkloadItems.size());
		assertTrue(mapWorkloadItems.containsKey("Behavior_DifferentParameter"));

		HttpWorkloadItem workloadItem = mapWorkloadItems.get("Behavior_DifferentParameter");
		assertEquals(DriverType.HTTP, workloadItem.driverType());
		assertFalse(workloadItem.dataSources().nonEmpty());
		assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
		assertFalse(workloadItem.popularity().isDefined());

		
		// Check Operations
		List<Operation> operations = getJavaListOperation(workloadItem.operations().get());
		assertEquals(2, operations.size());
		
		assertEquals("selectUsingGET", operations.get(0).id());
		assertEquals("itemSelectionUsingPOST", operations.get(1).id());
		
		for(Operation operation : operations) {
			assertFalse(operation.jsonExtraction().isDefined());
			assertFalse(operation.regexExtraction().isDefined());
			assertFalse(operation.headers().isDefined());
			assertTrue(operation.urlParameter().isDefined());
		}
		
		Operation operationSelect = operations.get(0);
		Operation operationItem = operations.get(1);
		
		assertEquals(Protocol.HTTP, operationSelect.protocol());
		assertEquals(Protocol.HTTPS, operationItem.protocol());
		
		assertFalse(operationSelect.thinkTime().isDefined());
		assertFalse(operationItem.thinkTime().isDefined());
		
		assertEquals(Method.GET, operationSelect.method());
		assertEquals(Method.POST, operationItem.method());
		
		assertEquals("/shop/${product}/select/${color}", operationSelect.endpoint());
		assertEquals("/itemselection/${category}", operationItem.endpoint());
		
		assertFalse(operationSelect.body().isDefined());
		assertTrue(operationItem.body().isDefined());
		
		assertFalse(operationSelect.queryParameter().isDefined());
		assertTrue(operationItem.queryParameter().isDefined());
		
		// Test endpoint with only url parameter
		Map<String, Parameter> mapSelectUrlParameter = JavaConverters.mapAsJavaMap(operationSelect.urlParameter().get());
		assertTrue(mapSelectUrlParameter.containsKey("product"));
		assertTrue(mapSelectUrlParameter.containsKey("color"));
		
		List<String> parameterProduct = JavaConverters.seqAsJavaList(mapSelectUrlParameter.get("product").items());
		List<String> parameterColor = JavaConverters.seqAsJavaList(mapSelectUrlParameter.get("color").items());
		assertEquals(2, parameterProduct.size());	
		assertEquals(3, parameterColor.size());	
		
		assertEquals("car", parameterProduct.get(0));
		assertEquals("bike", parameterProduct.get(1));
		
		assertEquals("black", parameterColor.get(0));
		assertEquals("red", parameterColor.get(1));
		assertEquals("blue", parameterColor.get(2));
		
		// Test endpoint with different parameter types
		Map<String, Parameter> mapItemUrlParameter = JavaConverters.mapAsJavaMap(operationItem.urlParameter().get());
		assertTrue(mapItemUrlParameter.containsKey("category"));
		
		List<String> parameterCategory = JavaConverters.seqAsJavaList(mapItemUrlParameter.get("category").items());
		assertEquals(2, parameterCategory.size());	
		assertEquals("top", parameterCategory.get(0));
		assertEquals("bottom", parameterCategory.get(1));
		
		Map<String, Parameter> mapItemQueryParameter = JavaConverters.mapAsJavaMap(operationItem.queryParameter().get());
		assertTrue(mapItemQueryParameter.containsKey("id"));
		
		List<String> parameterId = JavaConverters.seqAsJavaList(mapItemQueryParameter.get("id").items());
		assertEquals(1, parameterId.size());	
		assertEquals("123", parameterId.get(0));
		
		assertTrue(operationItem.body().get() instanceof BodyForm);
		BodyForm bodyForm = (BodyForm) operationItem.body().get();
		Map<String, Parameter> mapItemBodyFormParameter = JavaConverters.mapAsJavaMap(bodyForm.body());
		assertTrue(mapItemBodyFormParameter.containsKey("price"));
		
		List<String> parameterPrice = JavaConverters.seqAsJavaList(mapItemBodyFormParameter.get("price").items());
		assertEquals(3, parameterPrice.size());	
		assertEquals("0.12", parameterPrice.get(0));
		assertEquals("12.34", parameterPrice.get(1));
		assertEquals("987.65", parameterPrice.get(2));
		
		final double[][] PROBABILITY  = 
		{{0.2, 0.8},
		{0.0, 0.0}};
				
		this.checkMix(workloadItem, PROBABILITY);
	}
	
	@Test
	public void testTransformToBenchFlowWithRegexParameters() {
		
		// Init test
		BehaviorModelGenerator behaviorModelGenerator = new BehaviorModelGenerator();
		ContinuITyIDPAGenerator idpaGenerator = new ContinuITyIDPAGenerator();
		
		BehaviorModel behaviorModel = behaviorModelGenerator.createBehaviorModel(4);
		Application application = idpaGenerator.setupApplication();

		ApplicationAnnotation annotation = idpaGenerator.setupAnnotation(application);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, application, annotation);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(1, mapWorkloadItems.size());
		assertTrue(mapWorkloadItems.containsKey("Behavior_RegexParameter"));

		HttpWorkloadItem workloadItem = mapWorkloadItems.get("Behavior_RegexParameter");
		assertEquals(DriverType.HTTP, workloadItem.driverType());
		assertFalse(workloadItem.dataSources().nonEmpty());
		assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
		assertFalse(workloadItem.popularity().isDefined());

		
		// Check Operations
		List<Operation> operations = getJavaListOperation(workloadItem.operations().get());
		assertEquals(3, operations.size());
		
		assertEquals("loginUsingPOST", operations.get(0).id());
		assertEquals("accountUsingPOST", operations.get(1).id());
		assertEquals("buyUsingGET", operations.get(2).id());
		
		for(Operation operation : operations) {
			assertFalse(operation.jsonExtraction().isDefined());
			assertFalse(operation.urlParameter().isDefined());
			assertEquals(Protocol.HTTP, operation.protocol());
		}
		
		Operation operationLogin = operations.get(0);		
		Operation operationAccount = operations.get(1);
		Operation operationBuy = operations.get(2);
		
		assertFalse(operationLogin.thinkTime().isDefined());
		assertFalse(operationAccount.thinkTime().isDefined());
		assertFalse(operationBuy.thinkTime().isDefined());
		
		assertEquals(Method.POST, operationLogin.method());
		assertEquals(Method.POST, operationAccount.method());
		assertEquals(Method.GET, operationBuy.method());
		
		assertEquals("/login", operationLogin.endpoint());
		assertEquals("/account", operationAccount.endpoint());
		assertEquals("/buy", operationBuy.endpoint());
		
		assertTrue(operationLogin.headers().isDefined());
		assertFalse(operationAccount.headers().isDefined());
		assertFalse(operationBuy.headers().isDefined());
		
		assertTrue(operationLogin.queryParameter().isDefined());
		assertFalse(operationAccount.queryParameter().isDefined());
		assertTrue(operationBuy.queryParameter().isDefined());
		
		assertFalse(operationLogin.body().isDefined());
		assertTrue(operationAccount.body().isDefined());
		assertFalse(operationBuy.body().isDefined());
		
		assertTrue(operationLogin.regexExtraction().isDefined());
		assertTrue(operationAccount.regexExtraction().isDefined());
		assertFalse(operationBuy.regexExtraction().isDefined());
		
		// Check headers
		Map<String, String> mapLoginHeaders = JavaConverters.mapAsJavaMap(operationLogin.headers().get());
		assertEquals(1, mapLoginHeaders.size());
		assertTrue(mapLoginHeaders.containsKey("Content-Type"));
		assertEquals("application/x-www-form-urlencoded", mapLoginHeaders.get("Content-Type"));
		
		// Check regex
		Map<String, Extraction> mapLoginRegex = JavaConverters.mapAsJavaMap(operationLogin.regexExtraction().get());
		assertEquals(2, mapLoginRegex.size());
		assertTrue(mapLoginRegex.containsKey("Input_extracted_token"));
		assertTrue(mapLoginRegex.containsKey("Input_extracted_item"));	
		
		Extraction extractionLoginToken = mapLoginRegex.get("Input_extracted_token");	
		assertEquals("<input name=\"object\" type=\"hidden\" value=\"(.*)\"/>", extractionLoginToken.pattern());	
		assertEquals(4, extractionLoginToken.matchNumber().get());
		assertEquals("OBJECT_NOT_FOUND", extractionLoginToken.fallbackValue().get());
		
		Extraction extractionLoginItem = mapLoginRegex.get("Input_extracted_item");
		assertEquals("<input name=\"item\" type=\"hidden\" value=\"(.*)\"/>", extractionLoginItem.pattern());
		assertEquals(1 , extractionLoginItem.matchNumber().get());
		assertEquals("NOT FOUND", extractionLoginItem.fallbackValue().get());
		
		Map<String, Extraction> mapAccountRegex = JavaConverters.mapAsJavaMap(operationAccount.regexExtraction().get());
		assertEquals(1, mapAccountRegex.size());
		assertTrue(mapAccountRegex.containsKey("Input_extracted_token"));
		
		Extraction extractionAccountToken = mapAccountRegex.get("Input_extracted_token");
		assertEquals("<input id=\"select\" name=\"object\" type=\"hidden\" value=\"(.*)\"/>", extractionAccountToken.pattern());
		
		assertEquals(1, extractionAccountToken.matchNumber().get());
		assertEquals("NOT FOUND", extractionAccountToken.fallbackValue().get());	
		
		// Check query parameter
		Map<String, Parameter> mapLoginQueryParameter = JavaConverters.mapAsJavaMap(operationLogin.queryParameter().get());
		assertEquals(2, mapLoginQueryParameter.size());
		assertTrue(mapLoginQueryParameter.containsKey("user"));
		assertTrue(mapLoginQueryParameter.containsKey("password"));
		
		List<String> parameterUser = JavaConverters.seqAsJavaList(mapLoginQueryParameter.get("user").items());
		assertEquals(2, parameterUser.size());
		assertEquals("foo", parameterUser.get(0));
		assertEquals("bar", parameterUser.get(1));
		
		List<String> parameterPassword = JavaConverters.seqAsJavaList(mapLoginQueryParameter.get("password").items());
		assertEquals(1, parameterPassword.size());
		assertEquals("admin", parameterPassword.get(0));
		
		// Check regex parameter
		Map<String, Parameter> mapBuyQueryParameter = JavaConverters.mapAsJavaMap(operationBuy.queryParameter().get());
		assertEquals(1, mapBuyQueryParameter.size());
		assertTrue(mapBuyQueryParameter.containsKey("token"));
		
		List<String> parameterObject = JavaConverters.seqAsJavaList(mapBuyQueryParameter.get("token").items());
		assertEquals(1, parameterObject.size());
		assertEquals("${Input_extracted_token}", parameterObject.get(0));
		
		assertTrue(operationAccount.body().get() instanceof BodyForm);
		BodyForm bodyForm = (BodyForm) operationAccount.body().get();
		Map<String, Parameter> mapAccountBodyParameter = JavaConverters.mapAsJavaMap(bodyForm.body());
		assertEquals(2, mapAccountBodyParameter.size());
		assertTrue(mapAccountBodyParameter.containsKey("token"));
		assertTrue(mapAccountBodyParameter.containsKey("item"));
		
		List<String> parameterToken = JavaConverters.seqAsJavaList(mapAccountBodyParameter.get("token").items());
		assertEquals(1, parameterToken.size());
		assertEquals("${Input_extracted_token}", parameterToken.get(0));
		
		List<String> parameterItem = JavaConverters.seqAsJavaList(mapAccountBodyParameter.get("item").items());
		assertEquals(1, parameterItem.size());
		assertEquals("${Input_extracted_item}", parameterItem.get(0));
		
		final double[][] PROBABILITY  = 
		{{0.0, 0.9, 0.0},
		{0.0, 0.0, 0.9},
		{0.0, 0.0, 0.0}};
				
		this.checkMix(workloadItem, PROBABILITY);
	}
	
	@Test
	public void testTransformToBenchFlowWithBodyParameters() {
		
		// Init test
		BehaviorModelGenerator behaviorModelGenerator = new BehaviorModelGenerator();
		ContinuITyIDPAGenerator idpaGenerator = new ContinuITyIDPAGenerator();
		
		BehaviorModel behaviorModel = behaviorModelGenerator.createBehaviorModel(5);
		Application application = idpaGenerator.setupApplication();
		ApplicationAnnotation annotation = idpaGenerator.setupAnnotation(application);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, application, annotation);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(1, mapWorkloadItems.size());
		assertTrue(mapWorkloadItems.containsKey("Behavior_BodyParameter"));

		HttpWorkloadItem workloadItem = mapWorkloadItems.get("Behavior_BodyParameter");
		assertEquals(DriverType.HTTP, workloadItem.driverType());
		assertFalse(workloadItem.dataSources().nonEmpty());
		assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
		assertFalse(workloadItem.popularity().isDefined());

		
		// Check Operations
		List<Operation> operations = getJavaListOperation(workloadItem.operations().get());
		assertEquals(2, operations.size());
		
		assertEquals("convertUsingPOST", operations.get(0).id());
		assertEquals("transformUsingPOST", operations.get(1).id());
		
		for(Operation operation : operations) {
			assertEquals(Method.POST, operation.method());
			assertFalse(operation.headers().isDefined());		
			assertFalse(operation.jsonExtraction().isDefined());			
			assertFalse(operation.urlParameter().isDefined());
			assertFalse(operation.queryParameter().isDefined());
			assertEquals(Protocol.HTTP, operation.protocol());
			assertFalse(operation.thinkTime().isDefined());
			
			assertTrue(operation.body().isDefined());
		}
		
		Operation operationConvert = operations.get(0);
		Operation operationTransform = operations.get(1);
		
		assertEquals("/convert", operationConvert.endpoint());
		assertEquals("/transform", operationTransform.endpoint());
		
		assertTrue(operationConvert.regexExtraction().isDefined());
		assertFalse(operationTransform.regexExtraction().isDefined());
				
		// Check regex
		Map<String, Extraction> mapConvertRegex = JavaConverters.mapAsJavaMap(operationConvert.regexExtraction().get());
		assertEquals(1, mapConvertRegex.size());
		assertTrue(mapConvertRegex.containsKey("Input_extracted_content_json"));
		
		Extraction extractionContent = mapConvertRegex.get("Input_extracted_content_json");	
		assertEquals("<div id=\"result\" value=\"(.*)\"/>", extractionContent.pattern());	
		assertEquals("NOT FOUND", extractionContent.fallbackValue().get());
		
		// Check regex parameter
		assertTrue(operationTransform.body().get() instanceof Body);
		Body transformBody = (Body) operationTransform.body().get();
		List<String> parameterBody = JavaConverters.seqAsJavaList(transformBody.body().items());
		assertEquals(1, parameterBody.size());
		assertEquals("${Input_extracted_content_json}", parameterBody.get(0));
		
		// Check body data
		assertTrue(operationConvert.body().get() instanceof Body);
		Body convertBody = (Body) operationConvert.body().get();
		List<String> parameterConvert = JavaConverters.seqAsJavaList(convertBody.body().items());
		assertEquals(2, parameterConvert.size());
		assertEquals("<xml><p>Hello</p></xml>", parameterConvert.get(0));
		assertEquals("<xml><div>World</div></xml>", parameterConvert.get(1));
		
		final double[][] PROBABILITY  = 
		{{0.0, 0.75},
		{0.0, 0.0}};
				
		this.checkMix(workloadItem, PROBABILITY);
	}
	
	@Test
	public void testTransformToBenchFlowWithInitialState() {
		
		// Init test
		BehaviorModelGenerator behaviorModelGenerator = new BehaviorModelGenerator();
		ContinuITyIDPAGenerator idpaGenerator = new ContinuITyIDPAGenerator();
		
		BehaviorModel behaviorModel = behaviorModelGenerator.createBehaviorModel(6);
		Application application = idpaGenerator.setupApplication();
		ApplicationAnnotation annotation = idpaGenerator.setupAnnotation(application);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, application, annotation);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(1, mapWorkloadItems.size());
		assertTrue(mapWorkloadItems.containsKey("Behavior_Initial"));

		HttpWorkloadItem workloadItem = mapWorkloadItems.get("Behavior_Initial");
		assertEquals(DriverType.HTTP, workloadItem.driverType());
		assertFalse(workloadItem.dataSources().nonEmpty());
		assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
		assertFalse(workloadItem.popularity().isDefined());

		// Check Operations
		List<Operation> operations = getJavaListOperation(workloadItem.operations().get());
		assertEquals(4, operations.size());
		
		assertEquals("INITIAL_STATE", operations.get(0).id());
		assertEquals("productUsingPOST", operations.get(1).id());
		assertEquals("logoutUsingGET", operations.get(2).id());
		assertEquals("startUsingOPTIONS", operations.get(3).id());
		
		for(Operation operation : operations) {
			assertFalse(operation.jsonExtraction().isDefined());
			assertFalse(operation.regexExtraction().isDefined());
			assertFalse(operation.headers().isDefined());
			assertFalse(operation.urlParameter().isDefined());
			assertFalse(operation.queryParameter().isDefined());
			assertFalse(operation.thinkTime().isDefined());
			assertFalse(operation.body().isDefined());
		}	
		
		Operation operationINITIAL = operations.get(0);
		Operation operationProduct = operations.get(1);
		Operation operationLogout = operations.get(2);
		Operation operationStart = operations.get(3);
		
		assertNull(operationINITIAL.endpoint());
		assertEquals("/product", operationProduct.endpoint());
		assertEquals("/logout", operationLogout.endpoint());
		assertEquals("/index.html/start", operationStart.endpoint());
		
		assertNull(operationINITIAL.protocol());
		assertEquals(Protocol.HTTP, operationProduct.protocol());
		assertEquals(Protocol.HTTP, operationLogout.protocol());
		assertEquals(Protocol.HTTP, operationStart.protocol());
		
		assertNull(operationINITIAL.method());
		assertEquals(Method.POST, operationProduct.method());
		assertEquals(Method.GET, operationLogout.method());
		assertEquals(Method.OPTIONS, operationStart.method());
		
		final double[][] PROBABILITY  = 
		{{0.0, 0.7, 0.2, 0.1},
		{0.0, 0.0, 0.0, 0.0},
		{0.0, 0.0, 0.0, 0.0},
		{0.0, 0.3, 0.0, 0.6}};
				
		this.checkMix(workloadItem, PROBABILITY);
	}
	
	@Test
	public void testModelToModelTransformation() {
		
		BenchFlowTestHelper testHelper = new BenchFlowTestHelper();

		ApplicationAnnotation annotationModel = testHelper.getIdpaModelFromFile(ApplicationAnnotation.class, BenchFlowTestHelper.TEST_IDPA_ANNOTATION_FILE_1);
		Application applicationModel = testHelper.getIdpaModelFromFile(Application.class, BenchFlowTestHelper.TEST_IDPA_APPLICATION_FILE_1);
		BehaviorModel behaviorModel = testHelper.getBehaviorModelFromFile(BenchFlowTestHelper.TEST_BEHAVIOR_FILE_1);
		
		ContinuITyModel continuITyModel = new ContinuITyModel(behaviorModel, applicationModel, annotationModel);
		HttpWorkload workload = transformater.transformToBenchFlow(continuITyModel);
		
		// Test workload and workload-items
		assertFalse(workload.dataSources().nonEmpty());
		assertFalse(workload.operations().nonEmpty());
		assertTrue(workload.workloads().nonEmpty());
		assertFalse(workload.sutVersion().isDefined());

		Map<String, HttpWorkloadItem> mapWorkloadItems = JavaConverters.mapAsJavaMap(workload.workloads());
		assertEquals(3, mapWorkloadItems.size());
		
		assertTrue(mapWorkloadItems.containsKey("behavior_model0"));
		assertTrue(mapWorkloadItems.containsKey("behavior_model1"));
		assertTrue(mapWorkloadItems.containsKey("behavior_model2"));

		for(HttpWorkloadItem workloadItem : mapWorkloadItems.values()) {
			assertEquals(DriverType.HTTP, workloadItem.driverType());
			assertFalse(workloadItem.dataSources().nonEmpty());
			assertEquals(InterOperationsTimingType.FIXED_TIME, workloadItem.interOperationTimings().get());
			assertTrue(workloadItem.popularity().isDefined());
		}
		
		// Check operations		
		for(Map.Entry<String, HttpWorkloadItem> entry : mapWorkloadItems.entrySet()) {
			List<Operation> operations = getJavaListOperation(entry.getValue().operations().get());
			
			if(entry.getKey().equals("behavior_model2")) {
				assertEquals(5, operations.size());			
				assertEquals("INITIAL_STATE", operations.get(0).id());
				assertEquals("buyUsingGET", operations.get(1).id());
				assertEquals("loginUsingPOST", operations.get(2).id());
				assertEquals("searchUsingGET", operations.get(3).id());
				assertEquals("shopUsingGET", operations.get(4).id());
			} else {
				assertEquals(4, operations.size());	
				assertEquals("loginUsingPOST", operations.get(0).id());
				assertEquals("buyUsingGET", operations.get(1).id());
				assertEquals("searchUsingGET", operations.get(2).id());
				assertEquals("shopUsingGET", operations.get(3).id());
			}
						
			for(Operation operation : operations) {
				if(operation.id().equals("INITIAL_STATE")) {
					assertNull(operation.protocol());
					assertNull(operation.endpoint());
					assertNull(operation.method());
					assertFalse(operation.headers().isDefined());
					assertFalse(operation.body().isDefined());
					assertFalse(operation.jsonExtraction().isDefined());
					assertFalse(operation.regexExtraction().isDefined());
					assertFalse(operation.urlParameter().isDefined());
					assertFalse(operation.queryParameter().isDefined());
					assertFalse(operation.thinkTime().isDefined());
					continue;
				}
				assertTrue(operation.headers().isDefined());
				assertFalse(operation.body().isDefined());
				assertFalse(operation.jsonExtraction().isDefined());
				assertEquals(Protocol.HTTP, operation.protocol());
				
				switch(operation.id()) {
					
				case "buyUsingGET":
					assertFalse(operation.regexExtraction().isDefined());
					assertFalse(operation.urlParameter().isDefined());
					assertFalse(operation.queryParameter().isDefined());
					assertTrue(operation.thinkTime().isDefined());
					assertEquals("/cart/mini", operation.endpoint());
					assertEquals(Method.GET, operation.method());
					break;
					
				case "loginUsingPOST":
					assertFalse(operation.regexExtraction().isDefined());
					assertFalse(operation.urlParameter().isDefined());
					assertTrue(operation.queryParameter().isDefined());
					assertEquals("/login_post.htm", operation.endpoint());
					assertEquals(Method.POST, operation.method());
					break;
					
				case "searchUsingGET":
					assertTrue(operation.regexExtraction().isDefined());
					assertFalse(operation.urlParameter().isDefined());
					assertTrue(operation.queryParameter().isDefined());
					assertTrue(operation.thinkTime().isDefined());
					assertEquals("/search", operation.endpoint());
					assertEquals(Method.GET, operation.method());
					break;
					
				case "shopUsingGET":
					assertFalse(operation.regexExtraction().isDefined());
					assertTrue(operation.urlParameter().isDefined());
					assertFalse(operation.queryParameter().isDefined());
					assertTrue(operation.thinkTime().isDefined());
					assertEquals("/shop-products/${category}", operation.endpoint());
					assertEquals(Method.GET, operation.method());
					break;
				}
			}	
		}
		
		final double[][] PROBABILITY_0  = 
		{{0.0, 0.9, 0.0, 0.0},
		{0.3, 0.0, 0.5, 0.2},
		{0.0, 0.0, 0.7, 0.0},
		{0.0, 0.0, 0.0, 0.7}};
		
		final double[][] PROBABILITY_1  = 
		{{0.0, 0.3, 0.0, 0.7},
		{0.0, 0.0, 0.75, 0.0},
		{0.0, 0.7, 0.2, 0.0},
		{0.0, 0.0, 0.0, 0.0}};
		
		final double[][] PROBABILITY_2  = 
		{{0.0, 0.0, 0.1, 0.6, 0.2},
		{0.0, 0.0, 0.0, 0.6, 0.2},
		{0.0, 0.0, 0.0, 0.0, 0.6},
		{0.0, 0.0, 0.0, 0.0, 0.7},
		{0.0, 0.4, 0.0, 0.2, 0.0}};
		
		this.checkMix(mapWorkloadItems.get("behavior_model0"), PROBABILITY_0);
		this.checkMix(mapWorkloadItems.get("behavior_model1"), PROBABILITY_1);
		this.checkMix(mapWorkloadItems.get("behavior_model2"), PROBABILITY_2);
		
	}
	
	private void checkMix(HttpWorkloadItem workloadItem, final double[][] PROBABILITY) {
		
		final int SIZE = PROBABILITY.length;
		
		// Check mix
		assertTrue(workloadItem.mix().isDefined());
		Mix mix = workloadItem.mix().get();
		
		assertFalse(mix.maxDeviation().isDefined());
		assertTrue(mix.mix() instanceof MatrixMix);
		
		MatrixMix matrixMix = (MatrixMix) mix.mix();
		List<Seq<Percent>> listMatrixMix = JavaConverters.seqAsJavaList(matrixMix.mix());
		assertEquals(SIZE, listMatrixMix.size());
		listMatrixMix.forEach(m -> assertEquals(SIZE, m.size()));
		
		for(int i = 0; i < SIZE; i++) {
			List<Percent> convertedListMatrixMix = JavaConverters.seqAsJavaList(listMatrixMix.get(i));
			for(int j = 0; j < SIZE; j++) {
				assertEquals(PROBABILITY[i][j], convertedListMatrixMix.get(j).underlying(), 0.000001);
			}		
		}
	}

	
	private List<Operation> getJavaListOperation(scala.collection.immutable.List<Operation> scalaList){
		return new ArrayList<>(JavaConverters.asJavaCollection(scalaList));
	}
}
