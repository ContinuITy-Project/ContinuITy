package org.continuity.workload.dsl.annotation;

/**
 * @author Henning Schulz
 *
 */
public abstract class AbstractAnnotationElement implements AnnotationElement {

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
