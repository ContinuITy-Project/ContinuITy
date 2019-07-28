package org.continuity.dsl.context.timespec;

/**
 * Applies if the specified numerical or string variable is unequal to the specified value.
 *
 * @author Henning Schulz
 *
 */
public class UnequalSpecification extends AbstractEqualitySpecification {

	@Override
	public boolean negateElasticQuery() {
		return true;
	}

}
