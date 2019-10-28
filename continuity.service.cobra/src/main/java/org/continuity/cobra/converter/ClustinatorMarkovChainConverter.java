package org.continuity.cobra.converter;

import java.util.Collections;
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
		this.endpoints = endpoints == null ? Collections.emptyList() : endpoints;
		this.n = endpoints == null ? 0 : endpoints.size();
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
			double sumProb = 0;

			for (String to : endpoints) {
				RelativeMarkovTransition transition = markovChain.getTransition(from, to);
				double prob = transition.getProbability();

				if ((j == (n - 1)) && (sumProb < 0.1)) {
					// ensure each line summarizes to 1
					// (wouldn't be the case if state 'from' does not exist)
					prob = 1 - sumProb;
				}

				array[transitionToIndex(i, j)] = prob;

				sumProb += prob;
				j++;
			}

			i++;
		}

		return array;
	}

	/**
	 * Converts a map of arrays as returned by the clustinator to a {@link MarkovBehaviorModel}.
	 *
	 * @param probs
	 *            The map of probability arrays. Each array needs to have exactly {@code n * n}
	 *            elements ({@code n} = number of endpoints).
	 * @param thinktimeMeans
	 *            The map of think time mean arrays. The keys need to correspond to the keys of
	 *            probs. Each array needs to have exactly {@code n * n} elements.
	 * @param thinktimeVariances
	 *            The map of think time variance arrays. The keys need to correspond to the keys of
	 *            probs. Each array needs to have exactly {@code n * n} elements.
	 * @return The behavior model.
	 */
	public MarkovBehaviorModel convertArrays(Map<String, double[]> probs, Map<String, double[]> thinktimeMeans, Map<String, double[]> thinktimeVariances) {
		MarkovBehaviorModel model = new MarkovBehaviorModel();

		for (String behaviorId : probs.keySet()) {
			RelativeMarkovChain chain = convertArray(probs.get(behaviorId), thinktimeMeans.get(behaviorId), thinktimeVariances.get(behaviorId));
			chain.setId(behaviorId);
			model.addMarkovChain(chain);
		}

		return model;
	}

	/**
	 * Converts an array as returned by the clustinator to a Markov chain.
	 *
	 * @param probs
	 *            The probability array, which needs to have exactly {@code n * n} elements
	 *            ({@code n} = number of endpoints).
	 * @param thinktimeMeans
	 *            The think time mean array, which needs to have exactly {@code n * n} elements.
	 * @param thinktimeVariances
	 *            The think time variance array, which needs to have exactly {@code n * n} elements.
	 * @return The parsed Markov chain.
	 */
	public RelativeMarkovChain convertArray(double[] probs, double[] thinktimeMeans, double[] thinktimeVariances) {
		if (probs.length != (n * n)) {
			throw new IllegalArgumentException("Cannot convert probs array of length " + probs.length + " to a Markov chain with " + n + " states!");
		}
		if (thinktimeMeans.length != (n * n)) {
			throw new IllegalArgumentException("Cannot convert think time mean array of length " + thinktimeMeans.length + " to a Markov chain with " + n + " states!");
		}
		if (thinktimeVariances.length != (n * n)) {
			throw new IllegalArgumentException("Cannot convert thin ktime variance array of length " + thinktimeVariances.length + " to a Markov chain with " + n + " states!");
		}

		RelativeMarkovChain markovChain = new RelativeMarkovChain();

		int i = 0;

		for (String from : endpoints) {
			int j = 0;

			for (String to : endpoints) {
				int index = transitionToIndex(i, j);
				markovChain.setTransition(from, to, new RelativeMarkovTransition(probs[index], new NormalDistribution(thinktimeMeans[index], thinktimeVariances[index])));

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
