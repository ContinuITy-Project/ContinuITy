/**
 */
package org.continuity.idpa.annotation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.serialization.ValueExtractionDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an input that is to be extracted from the responses of one or several interfaces via
 * regular expressions or Json paths.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "initial", "extractions" })
public class ExtractedInput extends AbstractIdpaElement implements Input {

	@JsonProperty(value = "extractions")
	@JsonDeserialize(contentUsing = ValueExtractionDeserializer.class)
	private List<ValueExtraction> extractions;

	@JsonProperty(value = "initial")
	@JsonInclude(value = Include.NON_NULL)
	private String initialValue;

	/**
	 * Returns the extractions.
	 *
	 * @return The extractions.
	 */
	public List<ValueExtraction> getExtractions() {
		if (extractions == null) {
			extractions = new ArrayList<>();
		}

		return this.extractions;
	}

	/**
	 * Sets the extractions.
	 *
	 * @param extractions
	 *            The extractions.
	 */
	public void setExtractions(List<ValueExtraction> extractions) {
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
