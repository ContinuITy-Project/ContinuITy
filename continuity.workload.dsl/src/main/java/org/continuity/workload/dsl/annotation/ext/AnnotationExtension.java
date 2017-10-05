package org.continuity.workload.dsl.annotation.ext;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic extension of an annotation.
 *
 * @see AnnotationElementExtension
 *
 * @author Henning Schulz
 *
 */
public class AnnotationExtension {

	private Map<String, AnnotationElementExtension> extensions;

	/**
	 * Gets the element extensions.
	 *
	 * @return The element extensions.
	 */
	public Map<String, AnnotationElementExtension> getExtensions() {
		if (extensions == null) {
			extensions = new HashMap<>();
		}

		return this.extensions;
	}

	/**
	 * Sets the element extensions.
	 *
	 * @param extensions
	 *            New value for the element extensions.
	 */
	public void setExtensions(Map<String, AnnotationElementExtension> extensions) {
		this.extensions = extensions;
	}

	public void addExtension(AnnotationElementExtension extension) {
		getExtensions().put(extension.getId(), extension);
	}

}
