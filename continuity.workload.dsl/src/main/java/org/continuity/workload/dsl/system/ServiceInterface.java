/**
 */
package org.continuity.workload.dsl.system;

/**
 * Representation of an interface of a system that can be called.
 *
 * @author Henning Schulz
 *
 */
public interface ServiceInterface {

	/**
	 * Returns the name of the represented interface.
	 *
	 * @return The name of the represented interface.
	 */
	String getName();

	/**
	 * Sets the name of the represented interface.
	 *
	 * @param value
	 *            The name of the represented interface.
	 */
	void setName(String value);

}
