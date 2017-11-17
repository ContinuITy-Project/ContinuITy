package org.continuity.annotation.dsl.custom;

import java.util.HashMap;
import java.util.Map;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;
import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.WeakReference;

/**
 * Generic extension of an element of an annotation. Elements are references by the id. The
 * extension is specified by key-value-pairs.
 *
 * @author Henning Schulz
 *
 */
public class CustomAnnotationElement extends AbstractContinuityModelElement {

	private WeakReference<? extends ContinuityModelElement> reference;

	private Map<String, String> properties;

	/**
	 * Gets {@link #reference}.
	 *
	 * @return {@link #reference}
	 */
	public WeakReference<? extends ContinuityModelElement> getReference() {
		return this.reference;
	}

	/**
	 * Sets {@link #reference}.
	 *
	 * @param reference
	 *            New value for {@link #reference}
	 */
	public void setReference(WeakReference<? extends ContinuityModelElement> reference) {
		this.reference = reference;
	}

	/**
	 * Gets the properties.
	 *
	 * @return The properties.
	 */
	public Map<String, String> getProperties() {
		if (properties == null) {
			properties = new HashMap<>();
		}

		return this.properties;
	}

	/**
	 * Sets the properties.
	 *
	 * @param extensions
	 *            The properties.
	 */
	public void setProperties(Map<String, String> extensions) {
		this.properties = extensions;
	}

	/**
	 * Adds a property.
	 *
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public void addProperty(String key, String value) {
		getProperties().put(key, value);
	}

}
