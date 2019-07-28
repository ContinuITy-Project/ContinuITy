package org.continuity.dsl.context.influence;

/**
 * Multiplies a numerical variable with a factor.
 *
 * @author Henning Schulz
 *
 */
public class MultipliedInfluence extends AbstractWorkloadInfluence {

	private double with;

	public double getWith() {
		return with;
	}

	public void setWith(double with) {
		this.with = with;
	}

}
