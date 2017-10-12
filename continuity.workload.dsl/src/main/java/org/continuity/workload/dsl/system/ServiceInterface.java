/**
 */
package org.continuity.workload.dsl.system;

import java.util.List;

import org.continuity.workload.dsl.ContinuityModelElement;

/**
 * Representation of an interface of a system that can be called.
 *
 * @author Henning Schulz
 *
 */
public interface ServiceInterface<P extends Parameter> extends ContinuityModelElement {

	/**
	 * Returns representations of the parameters of the interface.
	 *
	 * @return The parameters.
	 */
	public List<P> getParameters();

}
