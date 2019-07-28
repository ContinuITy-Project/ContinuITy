package org.continuity.dsl.context.influence;

import org.continuity.dsl.StringOrNumeric;

/**
 * Sets the variable to a fixed value.
 *
 * @author Henning Schulz
 *
 */
public class FixedInfluence extends AbstractWorkloadInfluence {

	private StringOrNumeric to;

	public StringOrNumeric getValue() {
		return to;
	}

	public void setValue(StringOrNumeric value) {
		this.to = value;
	}

}
