package org.continuity.workload.dsl.annotation.ext;

import java.util.HashMap;
import java.util.Map;

import org.continuity.workload.dsl.annotation.AnnotationElement;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.annotation.visitor.AnnotationVisitor;

/**
 * Generic extension of an element of an annotation. Elements are references by the id. The
 * extension is specified by key-value-pairs.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationElementExtension {

	private String id;

	private AnnotationElement element = null;

	private Map<String, String> extensions;

	private final AnnotationVisitor visitor = new AnnotationVisitor(this::checkAndSetElement);

	public AnnotationElement resolveElement(SystemAnnotation annotation) {
		if (element == null) {
			visitor.visit(annotation);
			if (element == null) {
				throw new IllegalArgumentException("Annotation " + annotation + " did not contain any element with id " + id);
			}
		}

		return element;
	}

	private boolean checkAndSetElement(AnnotationElement element) {
		if ((id != null) && id.equals(element.getId())) {
			this.element = element;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            New value for the id.
	 */
	public void setId(String id) {
		this.id = id;
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
