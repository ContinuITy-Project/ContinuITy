package org.continuity.api.entities.artifact.markovbehavior;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Represents a transition from one Markov state to another. It holds the transition probability and
 * a {@link NormalDistribution} think time.
 *
 * @author Henning Schulz
 *
 */
public class RelativeMarkovTransition implements MarkovTransition {

	public static final double PRECISION = 0.00005;

	private static final String FORMAT = "%s; n(%s %s)";

	private static final DecimalFormat PROB_FORMAT = new DecimalFormat("0.0###", new DecimalFormatSymbols(Locale.US));
	private static final DecimalFormat TT_FORMAT = new DecimalFormat("0.####", new DecimalFormatSymbols(Locale.US));

	private double probability;

	private double count;

	private double radius;

	private NormalDistribution thinkTime;

	/**
	 * Creates an instance.
	 *
	 * @param probability
	 *            The transition probability.
	 * @param count
	 *            The (average) transition count.
	 * @param radius
	 *            The radius around count.
	 * @param thinkTime
	 *            A {@link NormalDistribution} as think time.
	 */
	public RelativeMarkovTransition(double probability, double count, double radius, NormalDistribution thinkTime) {
		this.probability = probability;
		this.count = count;
		this.radius = radius;
		this.thinkTime = thinkTime;
	}

	/**
	 * Creates an instance based on the think time mean and variance.
	 *
	 * @param probability
	 *            The transition probability.
	 * @param count
	 *            The (average) transition count.
	 * @param radius
	 *            The radius around count.
	 * @param meanThinkTime
	 *            The mean think time.
	 * @param varianceThinkTime
	 *            The variance of the think time.
	 */
	public RelativeMarkovTransition(double probability, double count, double radius, double meanThinkTime, double varianceThinkTime) {
		this(probability, count, radius, new NormalDistribution(meanThinkTime, varianceThinkTime));
	}

	/**
	 * Creates an instance with an unknown count.
	 *
	 * @param probability
	 *            The transition probability.
	 * @param thinkTime
	 *            A {@link NormalDistribution} as think time.
	 */
	public RelativeMarkovTransition(double probability, NormalDistribution thinkTime) {
		this(probability, -1, -1, thinkTime);
	}

	/**
	 * Creates an instance with an unknown count.
	 *
	 * @param probability
	 *            The transition probability.
	 * @param meanThinkTime
	 *            The mean think time.
	 * @param varianceThinkTime
	 *            The variance of the think time.
	 */
	public RelativeMarkovTransition(double probability, double meanThinkTime, double varianceThinkTime) {
		this(probability, -1, -1, meanThinkTime, varianceThinkTime);
	}

	/**
	 * Creates a zero transition, i.e., with transition probability and think time 0.
	 */
	public RelativeMarkovTransition() {
		this(0.0, 0.0, 0.0, new NormalDistribution());
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
	public static RelativeMarkovTransition concatenate(RelativeMarkovTransition first, NormalDistribution stateDuration, RelativeMarkovTransition second) {
		if ((first == null) && (second == null)) {
			return new RelativeMarkovTransition();
		} else if (first == null) {
			return second;
		} else if (second == null) {
			return first;
		}

		double probability = first.getProbability() * second.getProbability();
		double count = first.getCount() * second.getProbability();
		double radius = recalculateRadius(first, second, count);
		NormalDistribution thinkTime = NormalDistribution.add(first.getThinkTime(), stateDuration, second.getThinkTime());

		return new RelativeMarkovTransition(probability, count, radius, thinkTime);
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
	public static RelativeMarkovTransition merge(RelativeMarkovTransition first, RelativeMarkovTransition second) {
		if ((first == null) && (second == null)) {
			return new RelativeMarkovTransition();
		} else if (first == null) {
			return second;
		} else if (second == null) {
			return first;
		}

		double probability = first.getProbability() + second.getProbability();
		double count = ((first.getCount() >= 0) && (second.getCount() >= 0)) ? first.getCount() + second.getCount() : -1;
		double radius = recalculateRadius(first, second, count);
		NormalDistribution thinkTime = NormalDistribution.combine(first.getProbability() / probability, first.getThinkTime(), second.getProbability() / probability, second.getThinkTime());

		return new RelativeMarkovTransition(probability, count, radius, thinkTime);
	}

	private static double recalculateRadius(RelativeMarkovTransition first, RelativeMarkovTransition second, double count) {
		return Stream.of(first, second).filter(t -> (t.getRadius() >= 0) && (t.getCount() >= 0)).map(t -> Arrays.asList(t.getCount() + t.getRadius(), t.getCount() - t.getRadius()))
				.flatMap(List::stream).mapToDouble(x -> Math.abs(count - x)).max().orElse(-1);
	}

	/**
	 * Returns the transition probability. If the count is non-negative, it should be made sure that
	 * the probability and count are either both zero or non-zero.
	 *
	 * @return A {@code double} between 0.0 and 1.0.
	 */
	public double getProbability() {
		return probability;
	}

	/**
	 * Sets the transition probability. If the count is non-negative, it should be made sure that
	 * the probability and count are either both zero or non-zero.
	 *
	 * @param probability
	 *            A {@code double} between 0.0 and 1.0.
	 */
	public void setProbability(double probability) {
		this.probability = probability;
	}

	/**
	 * Returns the transition count, i.e., the (average) number of transitions per session. It can
	 * be negative, indicating it is unknown. If it is non-negative, iIt should be made sure that
	 * the probability and count are either both zero or non-zero.
	 *
	 * @return A {@code double}.
	 */
	public double getCount() {
		return count;
	}

	/**
	 * Sets the transition count, i.e., the (average) number of transitions per session. It can be
	 * negative, indicating it is unknown. If it is non-negative, iIt should be made sure that the
	 * probability and count are either both zero or non-zero.
	 *
	 * @param count
	 *            A {@code double}.
	 */
	public void setCount(double count) {
		this.count = count;
	}

	/**
	 * Returns the transition radius, i.e., the maximum transition count difference of a session to
	 * this behavior model. It can be negative, indicating it is unknown or undefined.
	 *
	 * @return A {@code double}.
	 */
	public double getRadius() {
		return radius;
	}

	/**
	 * Returns the transition radius, i.e., the maximum transition count difference of a session to
	 * this behavior model. It can be negative, indicating it is unknown or undefined.
	 *
	 * @param radius
	 *            A {@code double}.
	 */
	public void setRadius(double radius) {
		this.radius = radius;
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

	@Override
	public boolean hasZeroProbability() {
		return probability == 0;
	}

}
