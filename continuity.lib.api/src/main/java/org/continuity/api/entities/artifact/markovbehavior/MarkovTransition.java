package org.continuity.api.entities.artifact.markovbehavior;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Represents a transition from one Markov state to another. It holds the transition probability and
 * a {@link NormalDistribution} think time.
 *
 * @author Henning Schulz
 *
 */
public class MarkovTransition {

	private static final String FORMAT = "%s; n(%s %s)";

	private static final DecimalFormat PROB_FORMAT = new DecimalFormat("0.0###", new DecimalFormatSymbols(Locale.US));
	private static final DecimalFormat TT_FORMAT = new DecimalFormat("0.####", new DecimalFormatSymbols(Locale.US));

	private double probability;

	private NormalDistribution thinkTime;

	/**
	 * Creates an instance.
	 *
	 * @param probability
	 *            The transition probability.
	 * @param thinkTime
	 *            A {@link NormalDistribution} as think time.
	 */
	public MarkovTransition(double probability, NormalDistribution thinkTime) {
		this.probability = probability;
		this.thinkTime = thinkTime;
	}

	/**
	 * Creates an instance based on the think time mean and variance.
	 *
	 * @param probability
	 *            The transition probability.
	 * @param meanThinkTime
	 *            The mean think time.
	 * @param varianceThinkTime
	 *            The variance of the think time.
	 */
	public MarkovTransition(double probability, double meanThinkTime, double varianceThinkTime) {
		this.probability = probability;
		this.thinkTime = new NormalDistribution(meanThinkTime, varianceThinkTime);
	}

	/**
	 * Creates a zero transition, i.e., with transition probability and think time 0.
	 */
	public MarkovTransition() {
		this(0.0, new NormalDistribution());
	}

	/**
	 * Concatenates two transitions, i.e., by multiplying the transition probabilities and adding
	 * the think times. To be used when a Markov state is removed and the incoming and outgoing
	 * transitions need to be concatenated.
	 *
	 * @param first
	 *            The first transition. Can be {@code null}.
	 * @param stateDuration
	 *            Duration of the removed state. This duration will be added to the think time of
	 *            the concatenated transition.
	 * @param second
	 *            The second transition. Can be {@code null}.
	 * @return The concatenation of the transitions.
	 */
	public static MarkovTransition concatenate(MarkovTransition first, NormalDistribution stateDuration, MarkovTransition second) {
		if ((first == null) && (second == null)) {
			return new MarkovTransition();
		} else if (first == null) {
			return second;
		} else if (second == null) {
			return first;
		}

		double probability = first.getProbability() * second.getProbability();
		NormalDistribution thinkTime = NormalDistribution.add(first.getThinkTime(), stateDuration, second.getThinkTime());

		return new MarkovTransition(probability, thinkTime);
	}

	/**
	 * Merges two "parallel" transitions, i.e., by adding the transition probabilities and
	 * calculating the weighted sum of the think times. To be used when two parallel transitions
	 * with the same start and destination states exist. <br>
	 * <i>Please note that the sum of the think times is not a proper convolution here, but the
	 * closest we can get when sticking to normal distributions.</i>
	 *
	 * @param first
	 *            The first transition. Can be {@code null}.
	 * @param second
	 *            The second transition. Can be {@code null}.
	 * @return A merged transition.
	 */
	public static MarkovTransition merge(MarkovTransition first, MarkovTransition second) {
		if ((first == null) && (second == null)) {
			return new MarkovTransition();
		} else if (first == null) {
			return second;
		} else if (second == null) {
			return first;
		}

		double probability = first.getProbability() + second.getProbability();
		NormalDistribution thinkTime = NormalDistribution.combine(first.getProbability() / probability, first.getThinkTime(), second.getProbability() / probability, second.getThinkTime());

		return new MarkovTransition(probability, thinkTime);
	}

	/**
	 * Returns the transition probability.
	 *
	 * @return A {@code double} between 0.0 and 1.0.
	 */
	public double getProbability() {
		return probability;
	}

	/**
	 * Sets the transition probability.
	 *
	 * @param probability
	 *            A {@code double} between 0.0 and 1.0.
	 */
	public void setProbability(double probability) {
		this.probability = probability;
	}

	/**
	 * Returns the think time.
	 *
	 * @return A {@link NormalDistribution}
	 */
	public NormalDistribution getThinkTime() {
		return thinkTime;
	}

	/**
	 * Sets the think time.
	 *
	 * @param thinkTime
	 *            A {@link NormalDistribution}
	 */
	public void setThinkTime(NormalDistribution thinkTime) {
		this.thinkTime = thinkTime;
	}

	/**
	 *
	 * The format of the string representation is
	 * {@code probability; n(thinkTimeMean thinkTimeDeviation)}. <br>
	 * <br>
	 *
	 * {@inheritDoc}
	 *
	 */
	@Override
	public String toString() {
		return String.format(FORMAT, PROB_FORMAT.format(probability), TT_FORMAT.format(thinkTime.getMean()), TT_FORMAT.format(Math.sqrt(thinkTime.getVariance())));
	}

}
