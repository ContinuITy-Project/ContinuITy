package org.continuity.workload.dsl.annotation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Henning Schulz
 *
 */
public abstract class AbstractAnnotationElement implements AnnotationElement {

	@JsonProperty(value = "id", required = false)
	@JsonInclude(Include.NON_NULL)
	private String id = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            The new value for id.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasId() {
		return id != null;
	}

}
