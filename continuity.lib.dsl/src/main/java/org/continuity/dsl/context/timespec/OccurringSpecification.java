package org.continuity.dsl.context.timespec;

/**
 * Applies if the specified boolean variable occurs.
 *
 * @author Henning Schulz
 *
 */
public class OccurringSpecification extends AbstractOccurrenceSpecification {

	@Override
	public boolean negateElasticQuery() {
		return false;
	}

}
