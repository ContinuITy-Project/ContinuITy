/**
 */
package org.continuity.workload.dsl.system;

/**
 * Represents a parameter of an {@link HttpInterface}.
 *
 * @author Henning Schulz
 *
 */
public class HttpParameter implements Parameter {

	private String name;

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

	/**
	 * Returns the name of the parameter.
	 *
	 * @return The name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the parameter.
	 *
	 * @param name
	 *            The name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (parameterType: ");
		result.append(parameterType);
		result.append(", name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

}
