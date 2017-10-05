/**
 */
package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Holds possible input data. Several associated data sets can be specified by creating two
 * {@link DataInput} and adding them to the {@link DataInput#getAssociated()} of each other.
 *
 * @author Henning Schulz
 *
 */
public abstract class DataInput extends AbstractAnnotationElement implements Input {

	@JsonProperty(value = "associated")
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	@JsonInclude(Include.NON_EMPTY)
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
