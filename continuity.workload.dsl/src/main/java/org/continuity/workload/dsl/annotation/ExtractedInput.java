/**
 */
package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an input that is to be extracted from the responses of one or several interfaces via
 * regular expressions.
 *
 * @author Henning Schulz
 *
 */
public class ExtractedInput implements Input {

	private String name;

	private List<RegExExtraction> extractions;

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

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
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

}
