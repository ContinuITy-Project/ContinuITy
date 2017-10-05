/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.system.Parameter;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Specifies the input for a specific parameter.
 *
 * @author Henning Schulz
 *
 */
public class ParameterAnnotation extends AbstractAnnotationElement {

	@JsonProperty(value = "input")
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	private Input input;

	// TODO: Check if name attribute should be moved to Parameter
	@JsonProperty(value = "parameter")
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name")
	@JsonIdentityReference(alwaysAsId = true)
	private Parameter annotatedParameter;

	/**
	 * Gets the input.
	 *
	 * @return The input.
	 */
	public Input getInput() {
		return this.input;
	}

	/**
	 * Sets the input.
	 *
	 * @param input
	 *            The input.
	 */
	public void setInput(Input input) {
		this.input = input;
	}

	/**
	 * Gets the annotated parameter.
	 *
	 * @return The annotated parameter.
	 */
	public Parameter getAnnotatedParameter() {
		return this.annotatedParameter;
	}

	/**
	 * Sets the annotated parameter.
	 *
	 * @param annotatedParameter
	 *            The annotated parameter.
	 */
	public void setAnnotatedParameter(Parameter annotatedParameter) {
		this.annotatedParameter = annotatedParameter;
	}

}
