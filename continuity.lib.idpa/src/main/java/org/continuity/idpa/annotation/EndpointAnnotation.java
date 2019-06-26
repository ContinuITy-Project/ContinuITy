/**
 */
package org.continuity.idpa.annotation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.serialization.ModelSanitizers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Annotation of a {@link Endpoint}. Specifies the sources of the inputs.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "endpoint", "overrides", "parameter-annotations" })
@JsonDeserialize(converter = ModelSanitizers.InterfaceAnnotation.class)
public class EndpointAnnotation extends OverrideableAnnotation<PropertyOverrideKey.EndpointLevel> {

	@JsonProperty(value = "endpoint")
	private WeakReference<Endpoint<?>> annotatedEndpoint;

	@JsonProperty(value = "parameter-annotations")
	private List<ParameterAnnotation> parameterAnnotations;

	/**
	 * Gets the annotated endpoint.
	 *
	 * @return The annotated endpoint.
	 */
	public WeakReference<Endpoint<?>> getAnnotatedEndpoint() {
		return this.annotatedEndpoint;
	}

	/**
	 * Sets the annotated endpoint.
	 *
	 * @param annotatedEndpoint
	 *            The annotated endpoint.
	 */
	public void setAnnotatedEndpoint(WeakReference<Endpoint<?>> annotatedEndpoint) {
		this.annotatedEndpoint = annotatedEndpoint;
	}

	/**
	 * Gets the parameter annotations.
	 *
	 * @return The parameter annotations.
	 */
	public List<ParameterAnnotation> getParameterAnnotations() {
		if (parameterAnnotations == null) {
			parameterAnnotations = new ArrayList<>();
		}

		return parameterAnnotations;
	}

	/**
	 * Sets the parameter annotations.
	 *
	 * @param parameterAnnotations
	 *            New value for the parameter annotations.
	 */
	public void setParameterAnnotations(List<ParameterAnnotation> parameterAnnotations) {
		this.parameterAnnotations = parameterAnnotations;
	}

	/**
	 * Adds a parameter annotation.
	 *
	 * @param annotation
	 *            The annotation to be added.
	 */
	public void addParameterAnnotation(ParameterAnnotation annotation) {
		getParameterAnnotations().add(annotation);
	}

}
