/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.system.ServiceInterface;

/**
 * Annotation of a {@link ServiceInterface}. Specifies the sources of the inputs.
 *
 * @author Henning Schulz
 *
 */
public class InterfaceAnnotation {

	private ServiceInterface annotatedInterface;

	/**
	 * Gets the annotated interface.
	 *
	 * @return The annotated interface.
	 */
	public ServiceInterface getAnnotatedInterface() {
		return this.annotatedInterface;
	}

	/**
	 * Sets the annotated interface.
	 *
	 * @param annotatedInterface
	 *            The annotated interface.
	 */
	public void setAnnotatedInterface(ServiceInterface annotatedInterface) {
		this.annotatedInterface = annotatedInterface;
	}

}
