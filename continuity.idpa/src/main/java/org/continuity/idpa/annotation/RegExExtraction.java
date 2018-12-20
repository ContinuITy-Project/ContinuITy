/**
 */
package org.continuity.idpa.annotation;

import org.continuity.idpa.serialization.ModelSanitizers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an extraction of a value specified by a regular expression from the response of an
 * interface. The regular expression can specify one ore several groups that are to be extracted.
 * The template specifies how these groups should be combined. Finally, if there are several
 * matches, the matchNumber defines which one to take.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "from", "pattern", "response-key", "template", "match-number", "fallback" })
@JsonDeserialize(converter = ModelSanitizers.RegExExtraction.class)
public class RegExExtraction extends AbstractValueExtraction {

	private static final String DEFAULT_TEMPLATE = "(1)";

	@JsonProperty(value = "pattern")
	private String pattern;

	@JsonProperty(value = "template")
	@JsonInclude(value = Include.CUSTOM, valueFilter = TemplateValueFilter.class)
	private String template = DEFAULT_TEMPLATE;

	/**
	 * Gets the pattern used to extract the value.
	 *
	 * @return The pattern.
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * Sets the pattern used to extract the value.
	 *
	 * @param pattern
	 *            The pattern.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Gets the template. An extracted group with number {@code n} is referenced by {@code (n)}.
	 *
	 * @return The template.
	 */
	public String getTemplate() {
		return this.template;
	}

	/**
	 * Sets the template. An extracted group with number {@code n} can be referenced by {@code (n)}.
	 *
	 * @param template
	 *            The new template.
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	private static final class TemplateValueFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return DEFAULT_TEMPLATE.equals(obj);
		}

	}

}
