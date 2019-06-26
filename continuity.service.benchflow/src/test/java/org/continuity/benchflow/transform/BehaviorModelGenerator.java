package org.continuity.benchflow.transform;

import java.util.ArrayList;
import java.util.List;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;

public class BehaviorModelGenerator {

	public BehaviorModel createBehaviorModel(int behaviorSettings) {
			
		List<Behavior> behaviors = new ArrayList<Behavior>();
		
		if(behaviorSettings <= 2) {
			Behavior behavior = new Behavior();
			behavior.setProbability(0.2);
			behaviors.add(behavior);	
			
			BehaviorWithoutParameter basicBehavior = new BehaviorWithoutParameter();
			behavior.setMarkovStates(basicBehavior.createMarkovStates());	
			behavior.setInitialState(basicBehavior.getInitialState());
			behavior.setName(basicBehavior.getBehaviorName());
		}
		if(behaviorSettings == 2) {
			Behavior behavior = new Behavior();
			behavior.setProbability(0.8);
			behaviors.add(behavior);	
			
			BehaviorWithFormParameter basicBehavior = new BehaviorWithFormParameter();
			behavior.setMarkovStates(basicBehavior.createMarkovStates());	
			behavior.setInitialState(basicBehavior.getInitialState());
			behavior.setName(basicBehavior.getBehaviorName());
		}
		if(behaviorSettings >= 3) {
			TestBehavior basicBehavior = null;
			switch(behaviorSettings) {
			case 3:
				basicBehavior = new BehaviorWithDifferentParameters();
				break;
			case 4:
				basicBehavior = new BehaviorWithRegexParameters();
				break;
			case 5:
				basicBehavior = new BehaviorWithBodyParameters();
				break;
			case 6:
				basicBehavior = new BehaviorWithInitialState();
				break;
			} 
			Behavior behavior = new Behavior();
			behavior.setMarkovStates(basicBehavior.createMarkovStates());	
			behavior.setInitialState(basicBehavior.getInitialState());
			behavior.setName(basicBehavior.getBehaviorName());
			behavior.setProbability(0.7);
			behaviors.add(behavior);
		}
		BehaviorModel model = new BehaviorModel();
		model.setBehaviors(behaviors);
		return model;
	}
	
	private interface TestBehavior {
		public List<MarkovState> createMarkovStates();
		public String getBehaviorName();	
		public String getInitialState();
	}
	
	private class BehaviorWithoutParameter implements TestBehavior {
	
		public String getBehaviorName() {
			return "Behavior_WithoutParameter";
		}
		
		public String getInitialState() {
			return "startUsingOPTIONS";
		}
		
		public List<MarkovState> createMarkovStates(){
			
			List<MarkovState> markovStates = new ArrayList<MarkovState>();
			
			MarkovState state1 = createMarkovState("productUsingPOST");
			MarkovState state2 = createMarkovState("logoutUsingGET");
			
			MarkovState state3 = createMarkovState("startUsingOPTIONS");
			state3.setTransitions(createTransitions());
			
			markovStates.add(state1);
			markovStates.add(state2);
			markovStates.add(state3);
			
			return markovStates;
		}
		
		private List<Transition> createTransitions(){
			
			List<Transition> transitions = new ArrayList<Transition>();
			Transition transition1 = new Transition();
			transition1.setTargetState("productUsingPOST");
			transition1.setProbability(0.7);	
			transitions.add(transition1);
			
			Transition transition2 = new Transition();
			transition2.setTargetState("logoutUsingGET");
			transition2.setProbability(0.3);	
			transition2.setMean(250.0);
			transition2.setDeviation(42.0);
			transitions.add(transition2);
			
			return transitions;
		}
	}
	
	private class BehaviorWithFormParameter implements TestBehavior {
		
		public String getBehaviorName() {
			return "Behavior_FormParameter";
		}
		
		public String getInitialState() {
			return "productUsingPOST";
		}
		
		public List<MarkovState> createMarkovStates(){
			
			List<MarkovState> markovStates = new ArrayList<MarkovState>();
			
			MarkovState state1 = createMarkovState("productUsingPOST");
			state1.setTransitions(createTransitions());
			
			MarkovState state2 = createMarkovState("searchUsingPOST");
					
			markovStates.add(state1);
			markovStates.add(state2);
			
			return markovStates;
		}
		
		private List<Transition> createTransitions(){
			
			List<Transition> transitions = new ArrayList<Transition>();
			Transition transition1 = new Transition();
			transition1.setTargetState("searchUsingPOST");
			transition1.setProbability(0.9);	
			transitions.add(transition1);
			
			return transitions;
		}
	}
	
	private class BehaviorWithDifferentParameters implements TestBehavior {
		
		public String getBehaviorName() {
			return "Behavior_DifferentParameter";
		}
		
		public String getInitialState() {
			return "selectUsingGET";
		}
		
		public List<MarkovState> createMarkovStates(){
			
			List<MarkovState> markovStates = new ArrayList<MarkovState>();
			
			MarkovState state1 = createMarkovState("selectUsingGET");
			state1.setTransitions(createTransitions());
			
			MarkovState state2 = createMarkovState("itemSelectionUsingPOST");
					
			markovStates.add(state1);
			markovStates.add(state2);
			
			return markovStates;
		}
		
		private List<Transition> createTransitions(){
			
			List<Transition> transitions = new ArrayList<Transition>();
			Transition transition1 = new Transition();
			transition1.setTargetState("itemSelectionUsingPOST");
			transition1.setProbability(0.8);	
			transitions.add(transition1);
			
			Transition transition2 = new Transition();
			transition2.setTargetState("selectUsingGET");
			transition2.setProbability(0.2);	
			transitions.add(transition2);
			
			return transitions;
		}
	}
	
	private class BehaviorWithRegexParameters implements TestBehavior {
		
		public String getBehaviorName() {
			return "Behavior_RegexParameter";
		}
		
		public String getInitialState() {
			return "loginUsingPOST";
		}
		
		public List<MarkovState> createMarkovStates(){
			
			List<MarkovState> markovStates = new ArrayList<MarkovState>();
			
			MarkovState state1 = createMarkovState("loginUsingPOST");
			state1.setTransitions(createTransitions("accountUsingPOST"));
			
			MarkovState state2 = createMarkovState("accountUsingPOST");
			state2.setTransitions(createTransitions("buyUsingGET"));
			
			MarkovState state3 = createMarkovState("buyUsingGET");
					
			markovStates.add(state1);
			markovStates.add(state2);
			markovStates.add(state3);
			
			return markovStates;
		}
		
		private List<Transition> createTransitions(String target){
			
			List<Transition> transitions = new ArrayList<Transition>();
			Transition transition1 = new Transition();
			transition1.setTargetState(target);
			transition1.setProbability(0.9);	
			transitions.add(transition1);
			
			return transitions;
		}
	}
	
	
	private class BehaviorWithBodyParameters implements TestBehavior {
		
		public String getBehaviorName() {
			return "Behavior_BodyParameter";
		}
		
		public String getInitialState() {
			return "convertUsingPOST";
		}
		
		public List<MarkovState> createMarkovStates(){
			
			List<MarkovState> markovStates = new ArrayList<MarkovState>();
			
			MarkovState state1 = createMarkovState("convertUsingPOST");
			state1.setTransitions(createTransitions("transformUsingPOST"));
			
			MarkovState state2 = createMarkovState("transformUsingPOST");
					
			markovStates.add(state1);
			markovStates.add(state2);
			
			return markovStates;
		}
		
		private List<Transition> createTransitions(String target){
			
			List<Transition> transitions = new ArrayList<Transition>();
			Transition transition1 = new Transition();
			transition1.setTargetState(target);
			transition1.setProbability(0.75);	
			transitions.add(transition1);
			
			return transitions;
		}
	}
	
	private class BehaviorWithInitialState implements TestBehavior {
		
		public String getBehaviorName() {
			return "Behavior_Initial";
		}
		
		public String getInitialState() {
			return "INITIAL";
		}
		
		public List<MarkovState> createMarkovStates(){
			
			List<MarkovState> markovStates = new ArrayList<MarkovState>();
			
			MarkovState initialState = createMarkovState("INITIAL");		
			MarkovState state1 = createMarkovState("productUsingPOST");
			MarkovState state2 = createMarkovState("logoutUsingGET");		
			MarkovState state3 = createMarkovState("startUsingOPTIONS");	
			
			initialState.setTransitions(new ArrayList<Transition>());
			initialState.getTransitions().add(createTransitions("productUsingPOST", 0.7));
			initialState.getTransitions().add(createTransitions("logoutUsingGET", 0.2));
			initialState.getTransitions().add(createTransitions("startUsingOPTIONS", 0.1));
	
			state3.setTransitions(new ArrayList<Transition>());
			state3.getTransitions().add(createTransitions("startUsingOPTIONS", 0.6));
			state3.getTransitions().add(createTransitions("productUsingPOST", 0.3));

			markovStates.add(initialState);
			markovStates.add(state1);
			markovStates.add(state2);
			markovStates.add(state3);
			
			return markovStates;
		}
		
		private Transition createTransitions(String target, double probability){
			
			Transition transition = new Transition();
			transition.setTargetState(target);
			transition.setProbability(probability);	
			
			return transition;
		}
	}
	
	private MarkovState createMarkovState(String id) {
		MarkovState state = new MarkovState();
		state.setId(id);
		return state;
	}
	
}