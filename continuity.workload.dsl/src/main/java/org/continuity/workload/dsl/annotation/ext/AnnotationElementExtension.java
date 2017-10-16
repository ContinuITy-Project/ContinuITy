package org.continuity.workload.dsl.annotation.ext;

import java.util.HashMap;
import java.util.Map;

import org.continuity.workload.dsl.AbstractContinuityModelElement;
import org.continuity.workload.dsl.ContinuityModelElement;
import org.continuity.workload.dsl.WeakReference;

/**
 * Generic extension of an element of an annotation. Elements are references by the id. The
 * extension is specified by key-value-pairs.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationElementExtension extends AbstractContinuityModelElement {

	private WeakReference<? extends ContinuityModelElement> reference;

	private Map<String, String> extensions;

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
	 * Gets the extensions.
	 *
	 * @return The extensions.
	 */
	public Map<String, String> getExtensions() {
		if (extensions == null) {
			extensions = new HashMap<>();
		}

		return this.extensions;
	}

	/**
	 * Sets the extensions.
	 *
	 * @param extensions
	 *            The extensions.
	 */
	public void setExtensions(Map<String, String> extensions) {
		this.extensions = extensions;
	}

	/**
	 * Adds an extension.
	 *
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public void addExtension(String key, String value) {
		getExtensions().put(key, value);
	}

}
