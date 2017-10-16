package org.continuity.workload.dsl.visitor;

import java.util.function.Predicate;

import org.continuity.workload.dsl.ContinuityModelElement;

/**
 * @author Henning Schulz
 *
 */
public class ContinuityElementFinder extends ContinuityModelVisitor {

	/**
	 * @param operation
	 */
	public ContinuityElementFinder(Predicate<ContinuityModelElement> operation) {
		super(operation);
	}

}
