package org.continuity.wessbas.transform.benchflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;
import org.eclipse.emf.common.util.EList;
import org.junit.Before;
import org.junit.Test;

import m4jdsl.ApplicationModel;
import m4jdsl.ApplicationState;
import m4jdsl.BehaviorModelState;
import m4jdsl.M4jdslFactory;
import m4jdsl.NormallyDistributedThinkTime;
import m4jdsl.Property;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolState;
import m4jdsl.RelativeFrequency;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.WorkloadModel;
import m4jdsl.impl.M4jdslFactoryImpl;

public class BehaviorModelConverterTest {

	private WessbasToBehaviorModelConverter converter;
	private M4jdslFactory factory;
	
	@Before
	public void setUp() {
		converter = new WessbasToBehaviorModelConverter();
		factory = new M4jdslFactoryImpl();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConversionWithoutWorkloadModel() {
		converter.convertToBehaviorModel(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConversionWithoutBehaviorMix() {	
		WorkloadModel model = factory.createWorkloadModel();
		converter.convertToBehaviorModel(model);
	}
	
	@Test
	public void testConversionWithOneBehavior() {
		WorkloadModel inputModel = createWorkloadModel(0);
		BehaviorModel behaviorModel = converter.convertToBehaviorModel(inputModel);
		
		assertNotNull(behaviorModel);
		assertEquals(1, behaviorModel.getBehaviors().size());
		
		Behavior behavior = behaviorModel.getBehaviors().get(0);
		assertEquals("loginUsingPOST", behavior.getInitialState());
		assertEquals("behavior_model0", behavior.getName());
		assertEquals(1.0, behavior.getProbability(), 0.00000001);
		
		List<MarkovState> states = behavior.getMarkovStates();
		assertEquals(3, states.size());
		assertNotNull(states.stream().filter(m -> m.getId().equals("loginUsingPOST")).findFirst());
		assertNotNull(states.stream().filter(m -> m.getId().equals("productUsingGET")).findFirst());
		assertNotNull(states.stream().filter(m -> m.getId().equals("startUsingOPTIONS")).findFirst());
		
		for(MarkovState state : states) {
			
			List<Transition> transitions = state.getTransitions();
			
			switch(state.getId()) {
			case "loginUsingPOST":
				assertEquals(1, transitions.size());
				assertEquals(5000.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(0.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(1.0, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("productUsingGET", transitions.get(0).getTargetState());
				break;
				
			case "productUsingGET":
				assertEquals(2, transitions.size());
				assertEquals(1100.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(300.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(0.35, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("productUsingGET", transitions.get(0).getTargetState());
				assertEquals(2000.0, transitions.get(1).getMean(), 0.0000001);
				assertEquals(510.0, transitions.get(1).getDeviation(), 0.0000001);
				assertEquals(0.65, transitions.get(1).getProbability(), 0.0000001);
				assertEquals("startUsingOPTIONS", transitions.get(1).getTargetState());
				break;
				
			case "startUsingOPTIONS":
				assertNull(transitions);
				break;
			}
		}	
	}
	
	@Test
	public void testConversionWithTwoBehavior() {
		WorkloadModel inputModel = createWorkloadModel(1);
		BehaviorModel behaviorModel = converter.convertToBehaviorModel(inputModel);
		
		assertNotNull(behaviorModel);
		assertEquals(2, behaviorModel.getBehaviors().size());
		
		Behavior behavior1 = behaviorModel.getBehaviors().get(0);
		assertEquals("loginUsingPOST", behavior1.getInitialState());
		assertEquals("behavior_model0", behavior1.getName());
		assertEquals(0.7, behavior1.getProbability(), 0.00000001);
		
		Behavior behavior2 = behaviorModel.getBehaviors().get(1);
		assertEquals("productUsingGET", behavior2.getInitialState());
		assertEquals("behavior_model2", behavior2.getName());
		assertEquals(0.3, behavior2.getProbability(), 0.00000001);
		
		List<MarkovState> states = behavior1.getMarkovStates();
		assertEquals(5, states.size());
		assertNotNull(states.stream().filter(m -> m.getId().equals("loginUsingPOST")).findFirst());
		assertNotNull(states.stream().filter(m -> m.getId().equals("productUsingGET")).findFirst());
		assertNotNull(states.stream().filter(m -> m.getId().equals("startUsingOPTIONS")).findFirst());
		assertNotNull(states.stream().filter(m -> m.getId().equals("searchUsingPOST")).findFirst());
		assertNotNull(states.stream().filter(m -> m.getId().equals("buyUsingGET")).findFirst());
		
		for(MarkovState state : states) {
			
			List<Transition> transitions = state.getTransitions();
			
			switch(state.getId()) {
			case "loginUsingPOST":
				assertEquals(1, transitions.size());
				assertEquals(5000.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(0.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(1.0, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("productUsingGET", transitions.get(0).getTargetState());
				break;
				
			case "productUsingGET":
				assertEquals(2, transitions.size());
				assertEquals(1100.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(300.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(0.35, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("productUsingGET", transitions.get(0).getTargetState());
				assertEquals(2000.0, transitions.get(1).getMean(), 0.0000001);
				assertEquals(510.0, transitions.get(1).getDeviation(), 0.0000001);
				assertEquals(0.65, transitions.get(1).getProbability(), 0.0000001);
				assertEquals("startUsingOPTIONS", transitions.get(1).getTargetState());
				break;
				
			case "startUsingOPTIONS":
				assertEquals(2, transitions.size());
				assertEquals(1200.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(300.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(0.45, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("startUsingOPTIONS", transitions.get(0).getTargetState());
				assertEquals(2000.0, transitions.get(1).getMean(), 0.0000001);
				assertEquals(520.0, transitions.get(1).getDeviation(), 0.0000001);
				assertEquals(0.55, transitions.get(1).getProbability(), 0.0000001);
				assertEquals("searchUsingPOST", transitions.get(1).getTargetState());
				break;
				
			case "searchUsingPOST":
				assertEquals(2, transitions.size());
				assertEquals(1300.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(300.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(0.55, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("searchUsingPOST", transitions.get(0).getTargetState());
				assertEquals(2000.0, transitions.get(1).getMean(), 0.0000001);
				assertEquals(530.0, transitions.get(1).getDeviation(), 0.0000001);
				assertEquals(0.45, transitions.get(1).getProbability(), 0.0000001);
				assertEquals("buyUsingGET", transitions.get(1).getTargetState());
				break;
				
			case "buyUsingGET":
				assertEquals(1, transitions.size());
				assertEquals(250.0, transitions.get(0).getMean(), 0.0000001);
				assertEquals(30.0, transitions.get(0).getDeviation(), 0.0000001);
				assertEquals(0.1, transitions.get(0).getProbability(), 0.0000001);
				assertEquals("buyUsingGET", transitions.get(0).getTargetState());
				break;
			}
		}	
	}
	
	public WorkloadModel createWorkloadModel(int settings) {		
		M4jdslFactory factory = new M4jdslFactoryImpl();
		
		WorkloadModel model = factory.createWorkloadModel();
		model.setApplicationModel(factory.createApplicationModel());
		
		ApplicationModel applicationModel = model.getApplicationModel();
		applicationModel.setSessionLayerEFSM(factory.createSessionLayerEFSM());	
		SessionLayerEFSM sessionLayerEFSM = applicationModel.getSessionLayerEFSM();

		m4jdsl.HTTPRequest httpRequest = createHTTPRequest(factory, sessionLayerEFSM, "stateId1", "PS1", "loginUsingPOST");		
		httpRequest.getProperties().add(createProperty(factory, "HTTPSampler.method", "POST"));
		httpRequest.getProperties().add(createProperty(factory, "HTTPSampler.path", "/login.html"));
		httpRequest.getProperties().add(createProperty(factory, "HTTPSampler.protocol", ""));

		m4jdsl.HTTPRequest httpRequest2 = createHTTPRequest(factory, sessionLayerEFSM, "stateId2", "PS2", "productUsingGET");		
		httpRequest2.getProperties().add(createProperty(factory, "HTTPSampler.protocol", "HTTPS"));
		httpRequest2.getProperties().add(createProperty(factory, "HTTPSampler.path", "./list/product.html"));
		
		m4jdsl.HTTPRequest httpRequest3 = createHTTPRequest(factory, sessionLayerEFSM, "stateId3", "PS3", "startUsingOPTIONS");		
		httpRequest3.getProperties().add(createProperty(factory, "HTTPSampler.method", "OPTIONS"));
		httpRequest3.getProperties().add(createProperty(factory, "HTTPSampler.protocol", "HTTP"));
		httpRequest3.getProperties().add(createProperty(factory, "HTTPSampler.path", "./index.html"));
		
		if(settings == 1) {	
			m4jdsl.HTTPRequest httpRequest4 = createHTTPRequest(factory, sessionLayerEFSM, "stateId4", "PS4", "searchUsingPOST");		
			httpRequest4.getProperties().add(createProperty(factory, "HTTPSampler.method", "POST"));
			httpRequest4.getProperties().add(createProperty(factory, "HTTPSampler.protocol", "HTTP"));
			httpRequest4.getProperties().add(createProperty(factory, "HTTPSampler.path", "./search.html"));
			
			m4jdsl.HTTPRequest httpRequest5 = createHTTPRequest(factory, sessionLayerEFSM, "stateId5", "PS5", "buyUsingGET");		
			httpRequest5.getProperties().add(createProperty(factory, "HTTPSampler.method", "GET"));
			httpRequest5.getProperties().add(createProperty(factory, "HTTPSampler.protocol", "HTTP"));
			httpRequest5.getProperties().add(createProperty(factory, "HTTPSampler.path", "./search.html"));		
		}
		
		model.setWorkloadIntensity(factory.createConstantWorkloadIntensity());
		model.setBehaviorMix(factory.createBehaviorMix());
		
		EList<m4jdsl.BehaviorModel> behaviorModels = model.getBehaviorModels();
		
		m4jdsl.BehaviorModel behaviorModel = initBehaviorModel("behavior_model0", 0);
		behaviorModels.add(behaviorModel);

		RelativeFrequency frequency = factory.createRelativeFrequency();
		frequency.setBehaviorModel(behaviorModel);
		model.getBehaviorMix().getRelativeFrequencies().add(frequency);
	
		initMarkovStates(behaviorModel.getMarkovStates(), sessionLayerEFSM.getApplicationStates());
		behaviorModel.setInitialState(behaviorModel.getMarkovStates().get(0));
		initTransitions(behaviorModel.getMarkovStates(), behaviorModel.getExitState());
			
		if(settings == 1) {
			frequency.setValue(0.7);
			
			m4jdsl.BehaviorModel behaviorModel2 = initBehaviorModel("behavior_model2", 1);
			behaviorModels.add(behaviorModel2);
			
			RelativeFrequency frequency2 = factory.createRelativeFrequency();
			frequency2.setBehaviorModel(behaviorModel2);
			frequency2.setValue(0.3);
			model.getBehaviorMix().getRelativeFrequencies().add(frequency2);
			
			initMarkovStates(behaviorModel2.getMarkovStates(), sessionLayerEFSM.getApplicationStates());
			behaviorModel2.setInitialState(behaviorModel2.getMarkovStates().get(1));
			initTransitions(behaviorModel2.getMarkovStates(), behaviorModel2.getExitState());
		}		
		
	
		return model;
	}
	
	private m4jdsl.BehaviorModel initBehaviorModel(String name, int exitStateId){
		m4jdsl.BehaviorModel behaviorModel = factory.createBehaviorModel();
		behaviorModel.setName(name);
		behaviorModel.setExitState(factory.createBehaviorModelExitState());
		behaviorModel.getExitState().setEId("MSId" + exitStateId + "exit");
		return behaviorModel;
	}
	
	private void initMarkovStates(EList<m4jdsl.MarkovState> markovStates, EList<ApplicationState> applicationStates){
		for(ApplicationState applicationState : applicationStates) {
			Service service = applicationState.getService();
			m4jdsl.MarkovState markovState = factory.createMarkovState();
			markovState.setEId("MSId" + service.getName() + "_Test");
			markovState.setService(service);			
			markovStates.add(markovState);
		}
	}
	
	private void initTransitions(EList<m4jdsl.MarkovState> markovStates, BehaviorModelState exitState) {
		
		for(int i = 1; i < markovStates.size() - 1; i++) {
			m4jdsl.MarkovState markovState = markovStates.get(i);
			
			EList<m4jdsl.Transition> outgoingTransitions = markovState.getOutgoingTransitions();			
			outgoingTransitions.add(createTransition(factory, 0.25 + (i * 0.1), markovState, 1000.0 + (i * 100), 300.0));
			outgoingTransitions.add(createTransition(factory, 0.75 - (i * 0.1), markovStates.get(i + 1), 2000.0, 500.0 + (i * 10)));
		}
		
		EList<m4jdsl.Transition> outgoingTransitionsStart = markovStates.get(0).getOutgoingTransitions();			
		outgoingTransitionsStart.add(createTransition(factory, 1, markovStates.get(1), 5000.0, 0.0));
		
		m4jdsl.MarkovState lastState = markovStates.get(markovStates.size() - 1);
		EList<m4jdsl.Transition> outgoingTransitionsLast = lastState.getOutgoingTransitions();		
		if(markovStates.size() > 3) {
			outgoingTransitionsLast.add(createTransition(factory, 0.1, lastState, 250.0, 30.0));
			outgoingTransitionsLast.add(createTransition(factory, 0.9, exitState, 0.0, 0.0));
		} else {
			outgoingTransitionsLast.add(createTransition(factory, 1, exitState, 0.0, 0.0));
		}		
	}
	
	
	private m4jdsl.Transition createTransition(M4jdslFactory factory, double probability, BehaviorModelState targetState, double mean, double deviation) {
		m4jdsl.Transition transition = factory.createTransition();
		transition.setProbability(probability);
		transition.setTargetState(targetState);
		
		NormallyDistributedThinkTime thinkTime = factory.createNormallyDistributedThinkTime();
		thinkTime.setMean(mean);
		thinkTime.setDeviation(deviation);
		transition.setThinkTime(thinkTime);
		
		return transition;
	}
	
	private m4jdsl.HTTPRequest createHTTPRequest(M4jdslFactory factory, SessionLayerEFSM sessionLayerEFSM, 
			String applicationStateId, String protocolStateId, String serviceName) {
		
		ApplicationState state = factory.createApplicationState();
		state.setEId(applicationStateId + "_" + serviceName);
		
		Service service = factory.createService();
		service.setName(serviceName);
		state.setService(service);	
		
		sessionLayerEFSM.getApplicationStates().add(state);		
		state.setProtocolDetails(factory.createProtocolLayerEFSM());
		
		ProtocolLayerEFSM protocolLayerEFSM = state.getProtocolDetails();
		ProtocolState protocolState = factory.createProtocolState();
		protocolState.setEId(protocolStateId);
		protocolLayerEFSM.getProtocolStates().add(protocolState);
		protocolLayerEFSM.setInitialState(protocolState);
		
		m4jdsl.HTTPRequest httpRequest = factory.createHTTPRequest();
		protocolState.setRequest(httpRequest);

		return httpRequest;
	}
	
	private Property createProperty(M4jdslFactory factory, String key, String value) {
		Property property = factory.createProperty();
		property.setKey(key);
		property.setValue(value);
		return property;
	}
}
