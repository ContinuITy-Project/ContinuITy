/**
 */
package org.continuity.idpa.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.continuity.idpa.AbstractIdpaElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a parameter of an {@link HttpEndpoint}.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "name", "parameter-type" })
public class HttpParameter extends AbstractIdpaElement implements Parameter {

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
		StringBuffer result = new StringBuffer();
		result.append(" (name: ");
		result.append(name);
		result.append(", parameterType: ");
		result.append(parameterType);
		result.append(", id: ");
		result.append(getId());
		result.append(')');
		return result.toString();
	}

	@Override
	public List<String> getDifferingProperties(Parameter otherParam) {
		if (!this.getClass().getName().equals(otherParam.getClass().getName())) {
			return Collections.singletonList("type");
		}

		HttpParameter other = (HttpParameter) otherParam;
		List<String> differences = new ArrayList<>();

		if (!Objects.equals(this.parameterType, other.parameterType)) {
			differences.add("parameter-type");
		}

		if (!Objects.equals(this.name, other.name)) {
			differences.add("name");
		}

		return differences;
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

		return getDifferingProperties((Parameter) obj).isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Parameter o) {
		return this.toString().compareTo(o.toString());
	}

}
