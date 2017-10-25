package org.continuity.workload.dsl.annotation.ext;

import java.util.HashMap;
import java.util.Map;

import org.continuity.workload.dsl.AbstractContinuityModelElement;

/**
 * Generic extension of an annotation.
 *
 * @see AnnotationExtensionElement
 *
 * @author Henning Schulz
 *
 */
public class AnnotationExtension extends AbstractContinuityModelElement {

	private Map<String, AnnotationExtensionElement> elements;

	/**
	 * Gets the extension elements.
	 *
	 * @return The extension elements.
	 */
	public Map<String, AnnotationExtensionElement> getElements() {
		if (elements == null) {
			elements = new HashMap<>();
		}

		return this.elements;
	}

	/**
	 * Sets the extension elements.
	 *
	 * @param elements
	 *            New value for the extension elements.
	 */
	public void setElements(Map<String, AnnotationExtensionElement> elements) {
		this.elements = elements;
	}

	/**
	 * Adds a new extension element.
	 *
	 * @param element
	 *            The extension element to be added.
	 */
	public void addElement(AnnotationExtensionElement element) {
		getElements().put(element.getId(), element);
	}

}
