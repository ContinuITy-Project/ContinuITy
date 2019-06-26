package org.continuity.wessbas.transform.benchflow;

import java.util.ArrayList;
import java.util.List;

import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;
import org.continuity.api.entities.artifact.BehaviorModel;

/**
 * Extraction of behavior information from a WESSBAS model.
 * 
 * @author Manuel Palenga
 *
 */
public class WessbasToBehaviorModelConverter {
	 
	/**
	 * Converts the passed workload model to an behavior model.
	 *  
	 * @param workloadModel
	 * 				The workload model.
	 * @return The extracted behavior of the provided workload model.
	 */
	public BehaviorModel convertToBehaviorModel(m4jdsl.WorkloadModel workloadModel) {

		if(workloadModel == null) {
			throw new IllegalArgumentException("WESSBAS model is not defined!");
		}
		
		if(workloadModel.getBehaviorMix() == null) {
			throw new IllegalArgumentException("Behavior mix is required!");
		}
		
		List<Behavior> behaviors = new ArrayList<Behavior>();
		
		for(m4jdsl.RelativeFrequency frequency : workloadModel.getBehaviorMix().getRelativeFrequencies()) {
			
			double probability = frequency.getValue();
			if(workloadModel.getBehaviorMix().getRelativeFrequencies().size() == 1) {
				probability = 1.0;
			}
			
			m4jdsl.BehaviorModel wessbasBehaviorModel = frequency.getBehaviorModel();
			
			String name = wessbasBehaviorModel.getName();
			String initialState = wessbasBehaviorModel.getInitialState().getService().getName();
			
			Behavior behavior = new Behavior();
			behavior.setName(name);
			behavior.setProbability(probability);
			behavior.setInitialState(initialState);			
			behavior.setMarkovStates(extractMarkovStates(wessbasBehaviorModel));
			
			behaviors.add(behavior);
		}

		BehaviorModel model = new BehaviorModel();
		model.setBehaviors(behaviors);
		return model;
	}
	
	private List<MarkovState> extractMarkovStates(m4jdsl.BehaviorModel wessbasBehaviorModel){
		
		List<MarkovState> markovStates = new ArrayList<MarkovState>();
		
		for(m4jdsl.MarkovState wessbasState : wessbasBehaviorModel.getMarkovStates()) {
			
			String serviceName = wessbasState.getService().getName();
			
			MarkovState state = new MarkovState();
			state.setId(serviceName);
			markovStates.add(state);
			
			state.setTransitions(extractTransitions(wessbasState));
		}
		
		if(markovStates.isEmpty()) {
			return null;
		}
		
		return markovStates;
	}
	
	private List<Transition> extractTransitions(m4jdsl.MarkovState wessbasState){
		
		List<Transition> transitions = new ArrayList<Transition>();
		
		for(m4jdsl.Transition wessbasTransition : wessbasState.getOutgoingTransitions()) {
			
			m4jdsl.BehaviorModelState behaviorModelState = wessbasTransition.getTargetState();
			
			if(behaviorModelState instanceof m4jdsl.MarkovState) {
				m4jdsl.MarkovState wessbasMarkovState = (m4jdsl.MarkovState) behaviorModelState;
				
				String targetServiceName = wessbasMarkovState.getService().getName();
				double probability = wessbasTransition.getProbability();
				
				Transition transition = new Transition();
				transition.setTargetState(targetServiceName);
				transition.setProbability(probability);	
				transitions.add(transition);
				
				insertThinkTime(transition, wessbasTransition.getThinkTime());			
			}
		}
		
		if(transitions.isEmpty()) {
			return null;
		}
		
		return transitions;
	}
	
	private void insertThinkTime(Transition transition, m4jdsl.ThinkTime thinkTime) {
		if(thinkTime instanceof m4jdsl.NormallyDistributedThinkTime) {
			m4jdsl.NormallyDistributedThinkTime wessbasThinkTime = (m4jdsl.NormallyDistributedThinkTime) thinkTime;
			double mean = wessbasThinkTime.getMean();
			double deviation = wessbasThinkTime.getDeviation();
			transition.setMean(mean);
			transition.setDeviation(deviation);
		}
	}
	
}
