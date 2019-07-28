package org.continuity.dsl.context.influence;

/**
 * Decreases a numerical variable by an amount.
 *
 * @author Henning Schulz
 *
 */
public class DecreasedInfluence extends AbstractWorkloadInfluence {

	private double by;

	public double getBy() {
		return by;
	}

	public void setBy(double by) {
		this.by = by;
	}

}
