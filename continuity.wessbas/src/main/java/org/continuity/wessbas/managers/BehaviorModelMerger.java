package org.continuity.wessbas.managers;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;
import org.continuity.api.entities.artifact.ModularizedSessionLogs;
import org.continuity.api.entities.artifact.ProcessingTimeNormalDistributions;
import org.continuity.api.entities.artifact.SessionLogs;

public class BehaviorModelMerger {

	/**
	 * Exit state name
	 */
	private static final String EXIT_STATE_NAME = "$";

	/**
	 * Initial state name
	 */
	private static final String INITIAL_STATE_NAME = "INITIAL*";

	/**
	 * Limiter between root state name and sub state name
	 */
	private static final String STATE_NAME_LIMITER = "#";

	/**
	 * Merges/ replaces the sub markov chain of each root markov state into the root markov chain.
	 * 
	 * @param workloadModularizationManager
	 *            TODO
	 * @param rootBehaviorModel
	 *            The root behavior model
	 * @param modularizedBehaviorModelsPerMarkovState
	 *            The submarkovChain
	 */
	public void replaceMarkovStatesWithSubMarkovChains(Behavior rootBehaviorModel, Map<String, Pair<Behavior, ModularizedSessionLogs>> modularizedBehaviorModelsPerMarkovState) {
		HashMap<MarkovState, Collection<MarkovState>> replacingMarkovStates = new HashMap<MarkovState, Collection<MarkovState>>();

		for (String rootMarkovStateName : modularizedBehaviorModelsPerMarkovState.keySet()) {
			// Retrieve root markov state
			MarkovState rootMarkovState = rootBehaviorModel.getMarkovState(rootMarkovStateName);

			if (rootMarkovState.getId().equals(INITIAL_STATE_NAME) || rootMarkovState.getId().equals(EXIT_STATE_NAME)) {
				continue;
			}

			Behavior modularizedBehavioralModel = modularizedBehaviorModelsPerMarkovState.get(rootMarkovState.getId()).getLeft();
			Map<String, ProcessingTimeNormalDistributions> processingTimeNormalDistributionsMap = modularizedBehaviorModelsPerMarkovState.get(rootMarkovState.getId()).getRight()
					.getNormalDistributions();

			// Markov state can be removed completely; transitions have to be merged
			if (modularizedBehavioralModel == null) {
				removeCycle(rootMarkovState);
				skipMarkovState(rootBehaviorModel.getMarkovStates(), rootMarkovState, processingTimeNormalDistributionsMap);
				for (Collection<MarkovState> modularizedMarkovStates : replacingMarkovStates.values()) {
					skipMarkovState(modularizedMarkovStates, rootMarkovState, processingTimeNormalDistributionsMap);
				}
				replacingMarkovStates.put(rootMarkovState, Collections.emptyList());
				continue;
			}

			// Rename modularized initial state
			modularizedBehavioralModel.getMarkovState(INITIAL_STATE_NAME).setId(rootMarkovState.getId() + STATE_NAME_LIMITER + INITIAL_STATE_NAME);

			// Rename modularized exit state
			modularizedBehavioralModel.getMarkovState(EXIT_STATE_NAME).setId(rootMarkovState.getId() + STATE_NAME_LIMITER + EXIT_STATE_NAME);
			// Do it for all references
			modularizedBehavioralModel.getMarkovStates().stream().forEach(state -> state.getTransitions().stream().filter(t -> t.getTargetState().equals(EXIT_STATE_NAME))
					.forEach(t -> t.setTargetState(rootMarkovState.getId() + STATE_NAME_LIMITER + EXIT_STATE_NAME)));

			Map<String, MarkovState> markovStatesToAdd = modularizedBehavioralModel.getMarkovStates().stream().map(p -> new SimpleEntry<String, MarkovState>(p.getId(), p))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			// Merge transitions targeting the exit state with the transitions of the root markov
			// state
			removeCycle(rootMarkovState);
			skipModularizedExitState(markovStatesToAdd.values(), rootMarkovState, processingTimeNormalDistributionsMap);

			// Remove exit state of sub markov chain
			markovStatesToAdd.remove(rootMarkovState.getId() + STATE_NAME_LIMITER + EXIT_STATE_NAME);

			// Bend transitions to the initial state of the modularized markov chain, which target
			// the current markovState
			removeCycle(markovStatesToAdd.get(rootMarkovState.getId() + STATE_NAME_LIMITER + INITIAL_STATE_NAME));
			skipModularizedInitialState(rootBehaviorModel.getMarkovStates(), rootMarkovState, markovStatesToAdd.get(rootMarkovState.getId() + STATE_NAME_LIMITER + INITIAL_STATE_NAME),
					processingTimeNormalDistributionsMap);
			for (Collection<MarkovState> modularizedMarkovStates : replacingMarkovStates.values()) {
				skipModularizedInitialState(modularizedMarkovStates, rootMarkovState, markovStatesToAdd.get(rootMarkovState.getId() + STATE_NAME_LIMITER + INITIAL_STATE_NAME),
						processingTimeNormalDistributionsMap);
			}
			// Remove initial state of sub markov chain
			markovStatesToAdd.remove(rootMarkovState.getId() + STATE_NAME_LIMITER + INITIAL_STATE_NAME);

			// Add markov chains
			replacingMarkovStates.put(rootMarkovState, markovStatesToAdd.values());
		}

		// Remove old Markov State and add new ones
		replacingMarkovStates.entrySet().stream().forEach(e -> {
			rootBehaviorModel.getMarkovStates().remove(e.getKey());
			rootBehaviorModel.getMarkovStates().addAll(e.getValue());
		});

	}

	/**
	 * Searches for a cycle
	 * 
	 * @param markovStateName
	 *            the name of the markov state, which is going to be skipped
	 * @param outgoingTransitions
	 *            the outgoing transitions of the markov state, which is going to be skipped
	 * @return Returns the proportionate propability of the cycle and can be add to the remaining
	 *         transitions
	 */
	private void removeCycle(MarkovState replacedMarkovState) {
		for (Transition transition : replacedMarkovState.getTransitions()) {
			// findCycle
			if (transition.getTargetState().equals(replacedMarkovState.getId())) {
				replacedMarkovState.getTransitions().remove(transition);
				double proportionatePropability = transition.getProbability() / replacedMarkovState.getTransitions().size();
				for (Transition transi : replacedMarkovState.getTransitions()) {
					transi.setProbability(transi.getProbability() + proportionatePropability);
				}
				break;
			}
		}
	}

	/**
	 * Bends incoming transitions of a given markov state to the outgoing transitions of the markov
	 * state.
	 * 
	 * @param behaviorModel
	 * 
	 * @param markovState
	 */
	private void skipMarkovState(Collection<MarkovState> markovStates, MarkovState rootMarkovState, Map<String, ProcessingTimeNormalDistributions> processingTimeNormalDistributions) {
		for (MarkovState mState : markovStates) {
			for (Transition transition : mState.getTransitions()) {
				// If transition is a incoming transition of the given markov state and not a cycle;
				// markovState != mState
				if (transition.getTargetState().equals(rootMarkovState.getId()) && !mState.getId().equals(rootMarkovState.getId())) {
					// Delete the transitions and replace them by the merged ones
					mState.getTransitions().remove(transition);
					if (processingTimeNormalDistributions.containsKey("root")) {
						sumWithNormalDistribution(transition,
								processingTimeNormalDistributions.get("root").getPreprocessingTimeMean() + processingTimeNormalDistributions.get("root").getPostprocessingTimeMean(),
								processingTimeNormalDistributions.get("root").getPreprocessingTimeDeviation() + processingTimeNormalDistributions.get("root").getPostprocessingTimeDeviation());
					}

					joinOldTransitionsWithNewTransitions(mState.getTransitions(), sumTransitions(transition, rootMarkovState.getTransitions()));
					break;
				}
			}
		}
	}

	/**
	 * Bends incoming transitions of a given markov state to the outgoing transitions of the markov
	 * state.
	 * 
	 * @param behaviorModel
	 * 
	 * @param markovState
	 */
	private void skipModularizedExitState(Collection<MarkovState> markovStates, MarkovState rootMarkovState, Map<String, ProcessingTimeNormalDistributions> processingTimeNormalDistributionsMap) {
		for (MarkovState mState : markovStates) {
			for (Transition transition : mState.getTransitions()) {
				// If transition is a incoming transition of the given markov state and not a cycle;
				// markovState != mState
				if (transition.getTargetState().equals(rootMarkovState.getId() + STATE_NAME_LIMITER + EXIT_STATE_NAME) && !mState.getId().equals(rootMarkovState.getId())) {
					// Delete the transitions and replace them by the merged ones
					mState.getTransitions().remove(transition);
					String key = mState.getId().split("#")[1];
					if (processingTimeNormalDistributionsMap.containsKey(key)) {
						sumWithNormalDistribution(transition, processingTimeNormalDistributionsMap.get(key).getPostprocessingTimeMean(),
								processingTimeNormalDistributionsMap.get(key).getPostprocessingTimeDeviation());
					}
					joinOldTransitionsWithNewTransitions(mState.getTransitions(), sumTransitions(transition, rootMarkovState.getTransitions()));
					break;
				}
			}
		}
	}

	/**
	 * Bends incoming transitions of a given markov state to the outgoing transitions of the markov
	 * state.
	 * 
	 * @param behaviorModel
	 * 
	 * @param markovState
	 */
	private void skipModularizedInitialState(Collection<MarkovState> rootMarkovStates, MarkovState rootMarkovState, MarkovState initialMarkovState,
			Map<String, ProcessingTimeNormalDistributions> processingTimeNormalDistributionsMap) {
		for (MarkovState mState : rootMarkovStates) {
			for (Transition transition : mState.getTransitions()) {
				// If transition is a incoming transition of the given markov state and not a cycle;
				// markovState != mState
				if (transition.getTargetState().equals(rootMarkovState.getId()) && !mState.getId().equals(rootMarkovState.getId())) {
					// Delete the transitions and replace them by the merged ones
					mState.getTransitions().remove(transition);

					for (Transition outgoingInitialStateTransition : initialMarkovState.getTransitions()) {
						String key = outgoingInitialStateTransition.getTargetState().split("#")[1];
						if (processingTimeNormalDistributionsMap.containsKey(key)) {
							sumWithNormalDistribution(outgoingInitialStateTransition, processingTimeNormalDistributionsMap.get(key).getPreprocessingTimeMean(),
									processingTimeNormalDistributionsMap.get(key).getPreprocessingTimeDeviation());
						}
					}

					joinOldTransitionsWithNewTransitions(mState.getTransitions(), sumTransitions(transition, initialMarkovState.getTransitions()));
					break;
				}
			}
		}
	}

	/**
	 * Joins transitions and the corresponding normal distributions
	 * 
	 * @param oldTransitions
	 *            the old transitions of a specific markov state
	 * @param newTransitions
	 *            the new transitions for a specific markov state
	 */
	private void joinOldTransitionsWithNewTransitions(List<Transition> oldTransitions, List<Transition> newTransitions) {
		for (Transition newTransition : newTransitions) {
			boolean joined = false;
			for (Transition oldTransition : oldTransitions) {
				// check, weather the new transition has to be merged.
				if (oldTransition.getTargetState().equals(newTransition.getTargetState())) {
					// Conflict, transitions have to be joined
					double weightOldTransition = oldTransition.getProbability() / (oldTransition.getProbability() + newTransition.getProbability());
					double weightNewTransition = newTransition.getProbability() / (oldTransition.getProbability() + newTransition.getProbability());

					double joinedMean = (weightOldTransition * oldTransition.getMean()) + (weightNewTransition * newTransition.getMean());
					double joinedDeviation = Math
							.sqrt((Math.pow(weightOldTransition, 2) * Math.pow(oldTransition.getDeviation(), 2)) + (Math.pow(weightNewTransition, 2) * Math.pow(newTransition.getDeviation(), 2)));
					double joinedProbability = oldTransition.getProbability() + newTransition.getProbability();

					oldTransition.setMean(joinedMean);
					oldTransition.setDeviation(joinedDeviation);
					oldTransition.setProbability(joinedProbability);
					joined = true;
					break;
				}
			}
			// No conflicts, can be simply added as additional transition
			if (joined == false) {
				oldTransitions.add(newTransition);
			}
		}
	}

	/**
	 * Merges transitions and calculates new probabilities, means and deviations.
	 * 
	 * @param incomingTransitions
	 *            the incoming transitions
	 * @param outgoingTransitions
	 *            the outgoing transitions
	 * @return the merged transitions
	 */
	private List<Transition> sumTransitions(Transition incomingTransition, List<Transition> outgoingTransitions) {
		List<Transition> mergedTransitions = new ArrayList<Transition>();
		for (Transition outgoingTransition : outgoingTransitions) {
			if (!incomingTransition.getTargetState().equals(outgoingTransition.getTargetState())) {
				Transition mergedTransition = new Transition();

				// Merge transitions
				mergedTransition.setProbability(incomingTransition.getProbability() * outgoingTransition.getProbability());
				mergedTransition.setMean(incomingTransition.getMean() + outgoingTransition.getMean());
				mergedTransition.setDeviation(Math.sqrt(Math.pow(incomingTransition.getDeviation(), 2) + Math.pow(outgoingTransition.getDeviation(), 2)));
				mergedTransition.setTargetState(outgoingTransition.getTargetState());
				mergedTransitions.add(mergedTransition);
			}

		}
		return mergedTransitions;
	}

	/**
	 * Sums normal distribution to the normal distribution of the given transition
	 * 
	 * @param transition
	 *            the given transition
	 * @param mean
	 *            the mean of the normal distribution
	 * @param deviation
	 *            the deviation of the normal distribution
	 */
	private void sumWithNormalDistribution(Transition transition, Double mean, Double deviation) {
		transition.setMean(transition.getMean() + mean);
		transition.setDeviation(Math.sqrt(Math.pow(transition.getDeviation(), 2) + Math.pow(deviation, 2)));
	}
}