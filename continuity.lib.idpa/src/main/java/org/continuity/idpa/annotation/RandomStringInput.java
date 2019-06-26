package org.continuity.idpa.annotation;

import org.continuity.idpa.AbstractIdpaElement;

/**
 * Generates a random string based on a regular exception. Note that not all transformations to load
 * drivers support the full regular expression space. At least, templates like
 * {@code [A-Z]{5}-[0-9A-D]{4}} should be supported.
 *
 * @author Henning Schulz
 *
 */
public class RandomStringInput extends AbstractIdpaElement implements Input {

	private String template;

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

}
