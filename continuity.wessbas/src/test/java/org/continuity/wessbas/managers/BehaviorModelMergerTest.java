package org.continuity.wessbas.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.ModularizedSessionLogs;
import org.continuity.api.entities.artifact.ProcessingTimeNormalDistributions;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link BehaviorModelMerger}
 * 
 * @author Tobias Angerstein
 */
public class BehaviorModelMergerTest {
	private static final String STATE_NAME_LIMITER = "#";

	/**
	 * Tests
	 * {@link BehaviorModelMerger#replaceMarkovStatesWithSubMarkovChains(org.continuity.api.entities.artifact.BehaviorModel.Behavior, java.util.Map)}
	 * 
	 * @author Tobias Angerstein
	 *
	 */
	public static class ReplaceMarkovStatesWithSubMarkovChains {
		/**
		 * This method tests, weather a certain MarkovState is going to be skipped successfully.
		 */
		@Test
		public void testSkippingAMarkovState() {
			Map<String, Pair<Behavior, ModularizedSessionLogs>> replacingMarkovStates = new HashMap<String, Pair<Behavior, ModularizedSessionLogs>>();
			HashMap<String, ProcessingTimeNormalDistributions> normalDistributionsMap = new HashMap<String, ProcessingTimeNormalDistributions>();
			ProcessingTimeNormalDistributions processingTimeNormalDistributions = new ProcessingTimeNormalDistributions(Arrays.asList(3.0), Arrays.asList(3.0));
			normalDistributionsMap.put("root", processingTimeNormalDistributions);
			replacingMarkovStates.put("A", Pair.of(null, new ModularizedSessionLogs(normalDistributionsMap)));
			Behavior behaviorModel = generateRootBehaviorModel();
			
			BehaviorModelMerger merger = new BehaviorModelMerger();
			merger.replaceMarkovStatesWithSubMarkovChains(behaviorModel, replacingMarkovStates);
			
			Assert.assertEquals("Expected the behavior model to have 4 states", 4 , behaviorModel.getMarkovStates().size());
			Assert.assertEquals("Expected the initial state to have two transitions", 2, behaviorModel.getMarkovState("INITIAL*").getTransitions().size());
			Assert.assertEquals("Expected the initial state to have two transitions", 2, behaviorModel.getMarkovState("INITIAL*").getTransitions().size());
			Assert.assertEquals(0.5, behaviorModel.getMarkovState("INITIAL*").getTransitions().get(0).getProbability().doubleValue(), 0.000001);
			Assert.assertEquals(0.5, behaviorModel.getMarkovState("INITIAL*").getTransitions().get(1).getProbability().doubleValue(), 0.000001);
			Assert.assertEquals(8, behaviorModel.getMarkovState("INITIAL*").getTransitions().get(0).getMean().doubleValue(), 0.000001);
			Assert.assertEquals(8, behaviorModel.getMarkovState("INITIAL*").getTransitions().get(1).getMean().doubleValue(), 0.000001);
		}
		
		/**
		 * This method tests, weather a certain MarkovState is going to be replaced with a submarkov chain successfully.
		 */
		@Test
		public void testReplacingAMarkovState() {
			Map<String, Pair<Behavior, ModularizedSessionLogs>> replacingMarkovStates = new HashMap<String, Pair<Behavior, ModularizedSessionLogs>>();
			HashMap<String, ProcessingTimeNormalDistributions> normalDistributionsMap = new HashMap<String, ProcessingTimeNormalDistributions>();
			ProcessingTimeNormalDistributions processingTimeNormalDistributionsA = new ProcessingTimeNormalDistributions(Arrays.asList(3.0), Arrays.asList(4.0));
			ProcessingTimeNormalDistributions processingTimeNormalDistributionsB = new ProcessingTimeNormalDistributions(Arrays.asList(4.0), Arrays.asList(5.0));
			
			normalDistributionsMap.put("A", processingTimeNormalDistributionsA);
			normalDistributionsMap.put("B", processingTimeNormalDistributionsB);
			Behavior subBehaviorModelForB = generateSubBehaviorModel("B");
			replacingMarkovStates.put("B", Pair.of(subBehaviorModelForB, new ModularizedSessionLogs(normalDistributionsMap)));
			Behavior behaviorModel = generateRootBehaviorModel();
			
			BehaviorModelMerger merger = new BehaviorModelMerger();
			merger.replaceMarkovStatesWithSubMarkovChains(behaviorModel, replacingMarkovStates);
			
			Assert.assertEquals("Expected the behavior model to have 6 states", 6 , behaviorModel.getMarkovStates().size());
			Assert.assertEquals("Expected the markov state A to have a transition to B#A", "B#A", behaviorModel.getMarkovState("A").getTransitions().get(2).getTargetState());
			Assert.assertEquals(0.15, behaviorModel.getMarkovState("A").getTransitions().get(2).getProbability(), 0.00000001);
			Assert.assertEquals(0.15, behaviorModel.getMarkovState("A").getTransitions().get(3).getProbability(), 0.00000001);
			Assert.assertEquals(5, behaviorModel.getMarkovState("A").getTransitions().get(2).getMean(), 0.00000001);
			Assert.assertEquals(6, behaviorModel.getMarkovState("A").getTransitions().get(3).getMean(), 0.00000001);
			Assert.assertEquals("Expected the first transition of B#A to target C", "C", behaviorModel.getMarkovState("B#A").getTransitions().get(0).getTargetState());
			Assert.assertEquals("Expected the second transition of B#A to target the exit state", "$", behaviorModel.getMarkovState("B#A").getTransitions().get(1).getTargetState());
			Assert.assertEquals("Expected the first transition of B#B to target C", "C", behaviorModel.getMarkovState("B#B").getTransitions().get(0).getTargetState());
			Assert.assertEquals("Expected the second transition of B#B to target the exit state", "$", behaviorModel.getMarkovState("B#B").getTransitions().get(1).getTargetState());
			Assert.assertEquals(0.5, behaviorModel.getMarkovState("B#A").getTransitions().get(0).getProbability(), 0.00000001);
			Assert.assertEquals(0.5, behaviorModel.getMarkovState("B#A").getTransitions().get(1).getProbability(), 0.00000001);
			Assert.assertEquals(9, behaviorModel.getMarkovState("B#A").getTransitions().get(0).getMean(), 0.00000001);
			Assert.assertEquals(9, behaviorModel.getMarkovState("B#A").getTransitions().get(1).getMean(), 0.00000001);
			Assert.assertEquals(10, behaviorModel.getMarkovState("B#B").getTransitions().get(0).getMean(), 0.00000001);
			Assert.assertEquals(10, behaviorModel.getMarkovState("B#B").getTransitions().get(1).getMean(), 0.00000001);
		}
		
		/**
		 * This method tests, weather the join of two transitions is working properly.
		 */
		@Test
		public void testJoinOfTransitions() {
			Map<String, Pair<Behavior, ModularizedSessionLogs>> replacingMarkovStates = new HashMap<String, Pair<Behavior, ModularizedSessionLogs>>();
			HashMap<String, ProcessingTimeNormalDistributions> normalDistributionsMap = new HashMap<String, ProcessingTimeNormalDistributions>();
			ProcessingTimeNormalDistributions processingTimeNormalDistributions = new ProcessingTimeNormalDistributions(Arrays.asList(2.0), Arrays.asList(2.0));
			normalDistributionsMap.put("root", processingTimeNormalDistributions);
			replacingMarkovStates.put("B", Pair.of(null, new ModularizedSessionLogs(normalDistributionsMap)));
			Behavior behaviorModel = generateRootBehaviorModel();
			
			BehaviorModelMerger merger = new BehaviorModelMerger();
			merger.replaceMarkovStatesWithSubMarkovChains(behaviorModel, replacingMarkovStates);
			Assert.assertEquals(0.45, behaviorModel.getMarkovState("A").getTransitions().get(1).getProbability(), 0.00000001);
			Assert.assertEquals(4, behaviorModel.getMarkovState("A").getTransitions().get(1).getMean(), 0.00000001);
			Assert.assertEquals(0.40824829, behaviorModel.getMarkovState("A").getTransitions().get(1).getDeviation(), 0.00000001);
		}
	}

	/**
	 * Generates root Behavior Model
	 * 
	 * @return rootBehaviorModel
	 */
	private static Behavior generateRootBehaviorModel() {
		Behavior behavior = new Behavior();

		MarkovState initialState = new MarkovState();
		initialState.setId("INITIAL*");
		initialState.addTransition("A", 1, 0, 0);

		MarkovState intermediateState1 = new MarkovState();
		intermediateState1.setId("A");
		intermediateState1.addTransition("A", 0.4, 3, 0.5);
		intermediateState1.addTransition("B", 0.3, 2, 0.5);
		intermediateState1.addTransition("C", 0.3, 2, 0.5);

		MarkovState intermediateState2 = new MarkovState();
		intermediateState2.setId("B");
		intermediateState2.addTransition("C", 0.5, 2, 0.5);
		intermediateState2.addTransition("$", 0.5, 2, 0.5);

		MarkovState intermediateState3 = new MarkovState();
		intermediateState3.setId("C");
		intermediateState3.addTransition("$", 1, 0, 0);

		MarkovState exitState = new MarkovState();
		exitState.setId("$");
		exitState.setTransitions(Collections.emptyList());
		
		behavior.setMarkovStates(new ArrayList<MarkovState>(Arrays.asList(initialState, intermediateState1, intermediateState2, intermediateState3, exitState)));
		return behavior;

	}
	
	/**
	 * Generates root Behavior Model
	 * 
	 * @return rootBehaviorModel
	 */
	private static Behavior generateSubBehaviorModel(String rootMarkovStateName) {
		Behavior behavior = new Behavior();

		MarkovState initialState = new MarkovState();
		initialState.setId("INITIAL*");
		initialState.addTransition(rootMarkovStateName+STATE_NAME_LIMITER+"A", 0.5, 0, 0);
		initialState.addTransition(rootMarkovStateName+STATE_NAME_LIMITER+"B", 0.5, 0, 0);


		MarkovState intermediateState1 = new MarkovState();
		intermediateState1.setId(rootMarkovStateName+STATE_NAME_LIMITER+"A");
		intermediateState1.addTransition("$", 1, 3, 0.5);
		
		MarkovState intermediateState2 = new MarkovState();
		intermediateState2.setId(rootMarkovStateName+STATE_NAME_LIMITER+"B");
		intermediateState2.addTransition("$", 1, 3, 0.5);

		MarkovState exitState = new MarkovState();
		exitState.setId("$");
		exitState.setTransitions(Collections.emptyList());
		
		behavior.setMarkovStates(new ArrayList<MarkovState>(Arrays.asList(initialState, intermediateState1, intermediateState2, exitState)));
		return behavior;

	}
}
