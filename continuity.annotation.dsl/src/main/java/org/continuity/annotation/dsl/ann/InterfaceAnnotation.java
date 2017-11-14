/**
 */
package org.continuity.annotation.dsl.ann;

import java.util.ArrayList;
import java.util.List;

import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.json.ModelSanitizers;
import org.continuity.annotation.dsl.system.ServiceInterface;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Annotation of a {@link ServiceInterface}. Specifies the sources of the inputs.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "interface", "overrides", "parameter-annotations" })
@JsonDeserialize(converter = ModelSanitizers.InterfaceAnnotation.class)
public class InterfaceAnnotation extends OverrideableAnnotation<PropertyOverrideKey.InterfaceLevel> {

	@JsonProperty(value = "interface")
	private WeakReference<ServiceInterface<?>> annotatedInterface;

	@JsonProperty(value = "parameter-annotations")
	private List<ParameterAnnotation> parameterAnnotations;

	/**
	 * Gets the annotated interface.
	 *
	 * @return The annotated interface.
	 */
	public WeakReference<ServiceInterface<?>> getAnnotatedInterface() {
		return this.annotatedInterface;
	}

	/**
	 * Sets the annotated interface.
	 *
	 * @param annotatedInterface
	 *            The annotated interface.
	 */
	public void setAnnotatedInterface(WeakReference<ServiceInterface<?>> annotatedInterface) {
		this.annotatedInterface = annotatedInterface;
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
