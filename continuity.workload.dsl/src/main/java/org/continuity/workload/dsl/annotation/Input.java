/**
 */
package org.continuity.workload.dsl.annotation;

/**
 * Represents an input to a parameter.
 *
 * @author Henning Schulz
 *
 */
public interface Input {

	/**
	 * Returns the name of the input.
	 *
	 * @return the name.
	 */
	String getName();

	/**
	 * Sets the name of the input.
	 *
	 * @param name
	 *            the name.
	 */
	void setName(String name);

}
