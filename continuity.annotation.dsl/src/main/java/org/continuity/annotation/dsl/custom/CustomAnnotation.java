package org.continuity.annotation.dsl.custom;

import java.util.HashMap;
import java.util.Map;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;

/**
 * Generic extension of an annotation.
 *
 * @see CustomAnnotationElement
 *
 * @author Henning Schulz
 *
 */
public class CustomAnnotation extends AbstractContinuityModelElement {

	private Map<String, CustomAnnotationElement> elements;

	/**
	 * Gets the extension elements.
	 *
	 * @return The extension elements.
	 */
	public Map<String, CustomAnnotationElement> getElements() {
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
	public void setElements(Map<String, CustomAnnotationElement> elements) {
		this.elements = elements;
	}

	/**
	 * Adds a new extension element.
	 *
	 * @param element
	 *            The extension element to be added.
	 */
	public void addElement(CustomAnnotationElement element) {
		getElements().put(element.getId(), element);
	}

}
