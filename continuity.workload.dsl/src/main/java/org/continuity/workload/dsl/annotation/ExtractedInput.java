/**
 */
package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an input that is to be extracted from the responses of one or several interfaces via
 * regular expressions.
 *
 * @author Henning Schulz
 *
 */
public class ExtractedInput extends AbstractAnnotationElement implements Input {

	@JsonProperty(value = "extractions")
	private List<RegExExtraction> extractions;

	/**
	 * Returns the RegEx extractions.
	 *
	 * @return The extractions.
	 */
	public List<RegExExtraction> getExtractions() {
		if (extractions == null) {
			extractions = new ArrayList<>();
		}

		return this.extractions;
	}

	/**
	 * Sets the RegEx extractions.
	 *
	 * @param extractions
	 *            The extractions.
	 */
	public void setExtractions(List<RegExExtraction> extractions) {
		this.extractions = extractions;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (id: ");
		result.append(getId());
		result.append(')');
		return result.toString();
	}

}
