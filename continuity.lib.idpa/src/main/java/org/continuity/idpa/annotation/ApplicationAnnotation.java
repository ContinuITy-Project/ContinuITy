/**
 */
package org.continuity.idpa.annotation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Annotation of a {@link org.continuity.idpa.application.Application} representation. Holds manual
 * changes that should be kept while changing the system itself. <br>
 *
 * An annotation consists of a collection of inputs that can be either input data or extractions
 * from the responses of called interfaces. Additionally, it holds annotations of endpoints stating
 * which inputs map to which parameters.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "overrides", "inputs", "endpoint-annotations" })
public class ApplicationAnnotation extends OverrideableAnnotation<PropertyOverrideKey.Any> {

	@JsonProperty(value = "inputs")
	private List<Input> inputs;

	@JsonProperty(value = "endpoint-annotations")
	private List<EndpointAnnotation> endpointAnnotations;

	/**
	 * Returns the inputs.
	 *
	 * @return The inputs.
	 */
	public List<Input> getInputs() {
		if (inputs == null) {
			inputs = new ArrayList<>();
		}

		return this.inputs;
	}

	/**
	 * Sets the inputs.
	 *
	 * @param inputs
	 *            The inputs.
	 */
	public void setInputs(List<Input> inputs) {
		this.inputs = inputs;
	}

	/**
	 * Adds an input.
	 *
	 * @param input
	 *            The input to be added.
	 */
	public void addInput(Input input) {
		getInputs().add(input);
	}

	/**
	 * Returns the endpoint annotations.
	 *
	 * @return {@link #endpointAnnotations}
	 */
	public List<EndpointAnnotation> getEndpointAnnotations() {
		if (endpointAnnotations == null) {
			endpointAnnotations = new ArrayList<>();
		}

		return this.endpointAnnotations;
	}

	/**
	 * Sets the endpoint annotations.
	 *
	 * @param endpointAnnotations
	 *            The endpoint annotations.
	 */
	public void setEndpointAnnotations(List<EndpointAnnotation> endpointAnnotations) {
		this.endpointAnnotations = endpointAnnotations;
	}

}
