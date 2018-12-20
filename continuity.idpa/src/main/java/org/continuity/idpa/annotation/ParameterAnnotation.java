/**
 */
package org.continuity.idpa.annotation;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.serialization.ModelSanitizers;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Specifies the input for a specific parameter.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "parameter", "input", "overrides" })
@JsonDeserialize(converter = ModelSanitizers.ParameterAnnotation.class)
public class ParameterAnnotation extends OverrideableAnnotation<PropertyOverrideKey.ParameterLevel> {

	@JsonProperty(value = "input")
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	private Input input;

	@JsonProperty(value = "parameter")
	private WeakReference<Parameter> annotatedParameter;

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
	public WeakReference<Parameter> getAnnotatedParameter() {
		return this.annotatedParameter;
	}

	/**
	 * Sets the annotated parameter.
	 *
	 * @param annotatedParameter
	 *            The annotated parameter.
	 */
	public void setAnnotatedParameter(WeakReference<Parameter> annotatedParameter) {
		this.annotatedParameter = annotatedParameter;
	}

}
