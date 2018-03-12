/**
 */
package org.continuity.annotation.dsl.ann;

import java.util.ArrayList;
import java.util.List;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents an input that is to be extracted from the responses of one or several interfaces via
 * regular expressions.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "fallback", "extractions" })
public class ExtractedInput extends AbstractContinuityModelElement implements Input {

	@JsonProperty(value = "extractions")
	private List<RegExExtraction> extractions;

	@JsonProperty(value = "initial")
	@JsonInclude(value = Include.NON_NULL)
	private String initialValue;

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

	public String getInitialValue() {
		return initialValue;
	}

	public void setInitialValue(String initialValue) {
		this.initialValue = initialValue;
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
