package org.continuity.dsl.context.influence;

/**
 * Increases a numerical variable by an amount.
 * 
 * @author Henning Schulz
 *
 */
public class IncreasedInfluence extends AbstractWorkloadInfluence {

	private double by;

	public double getBy() {
		return by;
	}

	public void setBy(double by) {
		this.by = by;
	}

}
