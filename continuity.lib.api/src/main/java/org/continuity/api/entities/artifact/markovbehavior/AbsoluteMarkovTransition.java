package org.continuity.api.entities.artifact.markovbehavior;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a transition from one Markov state to another. It holds the absolute number of
 * transitions and a sample of think times.
 *
 * @author Henning Schulz
 *
 */
public class AbsoluteMarkovTransition implements MarkovTransition {

	private long numOccurrences;

	private List<Long> thinkTimes = new ArrayList<>();

	@Override
	public boolean hasZeroProbability() {
		return numOccurrences == 0;
	}

	/**
	 * Gets the number of occurrences this transition has been observed.
	 *
	 * @return The number as long.
	 */
	public long getNumOccurrences() {
		return numOccurrences;
	}

	/**
	 * Increments the transition, i.e., increments the number of occurrences by one and adds the
	 * think time.
	 *
	 * @param thinkTime
	 *            The observed think time.
	 */
	public void increment(long thinkTime) {
		this.numOccurrences++;
		this.thinkTimes.add(thinkTime);
	}

	/**
	 * Increments the transition by a number of occurrences, i.e., adds this number to the
	 * occurrences and adds the think times.
	 *
	 * @param num
	 *            The number of observed occurrences.
	 * @param thinkTimes
	 *            The observed think times.
	 */
	public void incrementBy(long num, List<Long> thinkTimes) {
		this.numOccurrences += num;
		this.thinkTimes.addAll(thinkTimes);
	}

	/**
	 * Transforms this absolute transition into a relative one.
	 *
	 * @param numOccurrencesFromState
	 *            The absolute number of transitions that have been observed from the outgoing state
	 *            to other states (including this one).
	 * @return The relative transition.
	 */
	public RelativeMarkovTransition toRelativeTransition(long numOccurrencesFromState) {
		double probability = (double) numOccurrences / (double) numOccurrencesFromState;
		NormalDistribution thinkTime = NormalDistribution.fromSample(thinkTimes.stream().mapToDouble(Long::doubleValue).toArray());

		return new RelativeMarkovTransition(probability, thinkTime);
	}

}
