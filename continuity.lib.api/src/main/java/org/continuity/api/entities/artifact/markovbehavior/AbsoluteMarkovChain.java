package org.continuity.api.entities.artifact.markovbehavior;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a Markov chain with absolute transitions. It is especially designed for constructing a
 * new Markov chain based on sessions.
 *
 * @author Henning Schulz
 *
 */
public class AbsoluteMarkovChain extends AbstractMarkovChain<AbsoluteMarkovTransition> {

	@Override
	protected AbsoluteMarkovTransition createEmptyTransition() {
		return new AbsoluteMarkovTransition();
	}

	/**
	 * Generates a {@link RelativeMarkovChain} by calculating the transition probabilities based on
	 * the collected transition occurrences and by calculating normal distributions of the think
	 * times.
	 *
	 * @return The relative Markov chain.
	 */
	public RelativeMarkovChain toRelativeMarkovChain() {
		Map<String, Map<String, RelativeMarkovTransition>> relativeTransitions = new TreeMap<>();

		for (Map.Entry<String, Map<String, AbsoluteMarkovTransition>> entry : getTransitions().entrySet()) {
			long sumTransitions = entry.getValue().values().stream().mapToLong(AbsoluteMarkovTransition::getNumOccurrences).sum();

			Map<String, RelativeMarkovTransition> outgoing = new TreeMap<>();
			entry.getValue().entrySet().forEach(e -> outgoing.put(e.getKey(), e.getValue().toRelativeTransition(sumTransitions)));

			relativeTransitions.put(entry.getKey(), outgoing);
		}

		return new RelativeMarkovChain(relativeTransitions);
	}

}
