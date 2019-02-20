package org.continuity.api.entities.artifact.markovbehavior;

import java.util.Arrays;
import java.util.stream.DoubleStream;

import javax.ws.rs.NotSupportedException;

/**
 * Represents a normal distribution of {@code double}.
 *
 * @author Henning Schulz
 *
 */
public class NormalDistribution {

	public static final NormalDistribution ZERO = new ZeroDistribution();

	private double mean;

	private double variance;

	/**
	 * Creates a new distribution.
	 *
	 * @param mean
	 *            The mean.
	 * @param variance
	 *            The variance.
	 */
	public NormalDistribution(double mean, double variance) {
		this.mean = mean;
		this.variance = variance;
	}

	/**
	 * Creates a new zero distribution, i.e., mean and variance are 0.
	 */
	public NormalDistribution() {
		this(0.0, 0.0);
	}

	/**
	 * Calculates the normal distribution from the given sample.
	 *
	 * @param sample
	 *            The sample.
	 * @return The resulting normal distribution.
	 */
	public static NormalDistribution fromSample(double... sample) {
		double mean = DoubleStream.of(sample).sum() / sample.length;
		double variance = DoubleStream.of(sample).map(x -> Math.pow(x - mean, 2)).sum() / (sample.length - 1);

		return new NormalDistribution(mean, variance);
	}

	/**
	 * Adds several normal distributions, i.e., calculates the convolution. The result will be
	 * another normal distribution.
	 *
	 * @param first
	 *            The first distribution to be added.
	 * @param second
	 *            The second distribution to be added.
	 * @param distributions
	 *            Further distributions to be added.
	 * @return The resulting normal distribution.
	 */
	public static NormalDistribution add(NormalDistribution first, NormalDistribution second, NormalDistribution... distributions) {
		double mean = first.getMean() + second.getMean() + Arrays.stream(distributions).mapToDouble(NormalDistribution::getMean).sum();
		double variance = first.getVariance() + second.getVariance() + Arrays.stream(distributions).mapToDouble(NormalDistribution::getVariance).sum();

		return new NormalDistribution(mean, variance);
	}

	/**
	 * Calculates a linear combination of two normal distributions.
	 *
	 * @param firstFactor
	 *            The factor of the first distribution.
	 * @param first
	 *            The first distribution to be added.
	 * @param secondFactor
	 *            The factor of the second distribution.
	 * @param second
	 *            The second distribution to be added.
	 * @return The resulting normal distribution.
	 */
	public static NormalDistribution combine(double firstFactor, NormalDistribution first, double secondFactor, NormalDistribution second) {
		return new NormalDistribution((firstFactor * first.getMean()) + (secondFactor * second.getMean()),
				(Math.pow(firstFactor, 2) * first.getVariance()) + (Math.pow(secondFactor, 2) * second.getVariance()));
	}

	/**
	 * Multiplies a factor to a normal distribution.
	 *
	 * @param factor
	 *            The factor to be multiplied.
	 * @param distribution
	 *            The normal distribution.
	 * @return The resulting normal distribution.
	 */
	public static NormalDistribution multiply(double factor, NormalDistribution distribution) {
		return new NormalDistribution(factor * distribution.getMean(), Math.pow(factor, 2) * distribution.getVariance());
	}

	/**
	 * Returns the mean.
	 *
	 * @return The mean.
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * Sets the mean.
	 *
	 * @param mean
	 *            The mean.
	 */
	public void setMean(double mean) {
		this.mean = mean;
	}

	/**
	 * Returns the variance.
	 *
	 * @return The variance.
	 */
	public double getVariance() {
		return variance;
	}

	/**
	 * Sets the variance.
	 *
	 * @param mean
	 *            The variance.
	 */
	public void setVariance(double variance) {
		this.variance = variance;
	}

	private static class ZeroDistribution extends NormalDistribution {

		@Override
		public void setMean(double mean) {
			throw new NotSupportedException("This is a constant ZERO distribution. Cannot set the mean!");
		}

		@Override
		public void setVariance(double variance) {
			throw new NotSupportedException("This is a constant ZERO distribution. Cannot set the variance!");
		}

	}

}
