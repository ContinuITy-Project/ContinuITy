/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.system.Parameter;

/**
 * Specifies the input for a specific parameter.
 *
 * @author Henning Schulz
 *
 */
public class ParameterAnnotation extends AbstractAnnotationElement {

	private Input input;

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
