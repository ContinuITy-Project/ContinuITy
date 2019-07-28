package org.continuity.dsl.context.timespec;

/**
 * Applies if the specified boolean variable does not occur.
 *
 * @author Henning Schulz
 *
 */
public class AbsentSpecification extends AbstractOccurrenceSpecification {

	@Override
	public boolean negateElasticQuery() {
		return true;
	}

}
