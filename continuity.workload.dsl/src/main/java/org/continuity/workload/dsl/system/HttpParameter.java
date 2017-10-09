/**
 */
package org.continuity.workload.dsl.system;

import org.continuity.workload.dsl.AbstractContinuityModelElement;

/**
 * Represents a parameter of an {@link HttpInterface}.
 *
 * @author Henning Schulz
 *
 */
public class HttpParameter extends AbstractContinuityModelElement implements Parameter {

	private HttpParameterType parameterType = HttpParameterType.REQ_PARAM;

	/**
	 * Returns the type of the parameter.
	 *
	 * @return The parameter type.
	 */
	public HttpParameterType getParameterType() {
		return this.parameterType;
	}

	/**
	 * Sets the type of the parameter.
	 *
	 * @param parameterType
	 *            The parameter type.
	 */
	public void setParameterType(HttpParameterType parameterType) {
		this.parameterType = parameterType;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (parameterType: ");
		result.append(parameterType);
		result.append(", id: ");
		result.append(getId());
		result.append(')');
		return result.toString();
	}

}
