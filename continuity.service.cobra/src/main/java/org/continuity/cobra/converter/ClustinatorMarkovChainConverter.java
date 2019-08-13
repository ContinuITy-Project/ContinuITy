package org.continuity.cobra.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovTransition;

/**
 * Converts Markov chains from and to the clustinator format.
 *
 * @author Henning Schulz
 *
 */
public class ClustinatorMarkovChainConverter {

	private final List<String> endpoints;

	private final int n;

	public ClustinatorMarkovChainConverter(List<String> endpoints) {
		this.endpoints = endpoints;
		this.n = endpoints.size();
	}

	/**
	 * Converts the Markov chains of a {@link MarkovBehaviorModel} into a map of arrays per ID that
	 * can be read by the clustinator.
	 *
	 * @param model
	 *            The behavior model.
	 * @return The formatted map of arrays. Each array will hold exactly {@code n * n} elements
	 *         ({@code n} = number of endpoints).
	 */
	public Map<String, double[]> convertBehaviorModel(MarkovBehaviorModel model) {
		Map<String, double[]> map = new HashMap<>();

		for (RelativeMarkovChain chain : model.getMarkovChains()) {
			map.put(chain.getId(), convertMarkovChain(chain));
		}

		return map;
	}

	/**
	 * Converts a Markov chain into an array that can be read by the clustinator.
	 *
	 * @param markovChain
	 *            The Markov chain.
	 * @return The formatted array, which will hold exactly {@code n * n} elements ({@code n} =
	 *         number of endpoints).
	 */
	public double[] convertMarkovChain(RelativeMarkovChain markovChain) {
		Set<String> unknownStates = new HashSet<>(markovChain.getRequestStates());
		unknownStates.removeAll(endpoints);
		unknownStates.forEach(s -> markovChain.removeState(s, NormalDistribution.ZERO));

		double[] array = new double[n * n];

		int i = 0;

		for (String from : endpoints) {
			int j = 0;

			for (String to : endpoints) {
				RelativeMarkovTransition transition = markovChain.getTransition(from, to);
				array[transitionToIndex(i, j)] = transition.getProbability();

				j++;
			}

			i++;
		}

		return array;
	}

	/**
	 * Converts an array as returned by the clustinator to a Markov chain.
	 *
	 * @param array
	 *            The array, which needs to have exactly {@code n * n} elements ({@code n} = number
	 *            of endpoints).
	 * @return The parsed Markov chain.
	 */
	public RelativeMarkovChain convertArray(double[] array) {
		if (array.length != (n * n)) {
			throw new IllegalArgumentException("Cannot convert array of length " + array.length + " to a Markov chain with " + n + " states!");
		}

		RelativeMarkovChain markovChain = new RelativeMarkovChain();

		int i = 0;

		for (String from : endpoints) {
			int j = 0;

			for (String to : endpoints) {
				double prob = array[transitionToIndex(i, j)];
				markovChain.setTransition(from, to, new RelativeMarkovTransition(prob, NormalDistribution.ZERO));

				j++;
			}

			i++;
		}

		return markovChain;
	}

	private int transitionToIndex(int from, int to) {
		return (from * n) + to;
	}

}
