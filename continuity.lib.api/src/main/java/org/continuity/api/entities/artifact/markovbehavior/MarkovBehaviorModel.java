package org.continuity.api.entities.artifact.markovbehavior;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common format for Markov-chain-based behavior models. Comprises several {@link RelativeMarkovChain}s.
 *
 * @author Henning Schulz
 *
 */
public class MarkovBehaviorModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(MarkovBehaviorModel.class);

	private List<RelativeMarkovChain> markovChains;

	/**
	 * Returns the comprised Markov chains.
	 *
	 * @return A list of {@link RelativeMarkovChain}.
	 */
	public List<RelativeMarkovChain> getMarkovChains() {
		return markovChains;
	}

	/**
	 * Sets the Markov chains.
	 *
	 * @param markovChains
	 *            A list of {@link RelativeMarkovChain}.
	 */
	public void setMarkovChains(List<RelativeMarkovChain> markovChains) {
		this.markovChains = markovChains;
	}

	/**
	 * Adds a Markov chain.
	 *
	 * @param chain
	 *            The {@link RelativeMarkovChain} to be added.
	 */
	public void addMarkovChain(RelativeMarkovChain chain) {
		if (markovChains == null) {
			markovChains = new ArrayList<>();
		}

		markovChains.add(chain);
	}

	/**
	 * Ensures that all contained {@link RelativeMarkovChain}s have the same set of states.
	 */
	public void synchronizeMarkovChains() {
		Map<String, Set<String>> statesPerChain = markovChains.stream().map(c -> Pair.of(c.getId(), new HashSet<>(c.getRequestStates()))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		Set<String> allStates = statesPerChain.values().stream().flatMap(Set::stream).distinct().collect(Collectors.toSet());

		for (RelativeMarkovChain chain : markovChains) {
			Set<String> remainingStates = new HashSet<>(allStates);
			remainingStates.removeAll(statesPerChain.get(chain.getId()));

			for (String state : remainingStates) {
				chain.addState(state);
				LOGGER.debug("Added state {} to Markov chain {}.", state, chain.getId());
			}
		}
	}

}
