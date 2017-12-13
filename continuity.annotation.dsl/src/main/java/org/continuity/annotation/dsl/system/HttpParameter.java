/**
 */
package org.continuity.annotation.dsl.system;

import java.util.Objects;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a parameter of an {@link HttpInterface}.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "name", "parameter-type" })
public class HttpParameter extends AbstractContinuityModelElement implements Parameter {

	@JsonProperty("parameter-type")
	private HttpParameterType parameterType = HttpParameterType.REQ_PARAM;

	@JsonProperty(value = "name", required = false)
	@JsonInclude(Include.NON_NULL)
	private String name;

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
	 * Gets the name of the parameter. Can be {@code null}.
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
	 *            New name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(", parameterType: ");
		result.append(parameterType);
		result.append(", id: ");
		result.append(getId());
		result.append(')');
		return result.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		if (!this.getClass().getName().equals(obj.getClass().getName())) {
			return false;
		}

		HttpParameter other = (HttpParameter) obj;

		return Objects.equals(this.parameterType, other.parameterType) && Objects.equals(this.name, other.name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Parameter o) {
		return this.toString().compareTo(o.toString());
	}

}
