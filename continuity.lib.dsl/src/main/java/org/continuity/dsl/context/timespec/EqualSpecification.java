package org.continuity.dsl.context.timespec;

/**
 * Applies if the specified numerical or string variable is equal to the specified value.
 *
 * @author Henning Schulz
 *
 */
public class EqualSpecification extends AbstractEqualitySpecification {

	@Override
	public boolean negateElasticQuery() {
		return false;
	}

}
