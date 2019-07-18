package org.continuity.api.entities.artifact.markovbehavior;

/**
 * Common interface for Markov transitions.
 *
 * @author Henning Schulz
 *
 */
public interface MarkovTransition {

	/**
	 *
	 * @return Whether this transition has a probability of zero.
	 */
	boolean hasZeroProbability();

}
