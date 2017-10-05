/**
 */
package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds possible input data. Several associated data sets can be specified by creating two
 * {@link DataInput} and adding them to the {@link DataInput#getAssociated()} of each other.
 *
 * @author Henning Schulz
 *
 */
public abstract class DataInput extends AbstractAnnotationElement implements Input {

	private List<DataInput> associated;

	/**
	 * Returns the associated inputs.
	 *
	 * @return The associated inputs.
	 */
	public List<DataInput> getAssociated() {
		if (associated == null) {
			associated = new ArrayList<>();
		}

		return this.associated;
	}

	/**
	 * Sets the associated inputs.
	 *
	 * @param associated
	 *            The associated inputs.
	 */
	public void setAssociated(List<DataInput> associated) {
		this.associated = associated;
	}

}
