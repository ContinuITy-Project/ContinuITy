/**
 */
package org.continuity.idpa.annotation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.idpa.AbstractIdpaElement;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds possible input data. Several associated data sets can be specified by creating two
 * {@link ListInput} and adding them to the {@link ListInput#getAssociated()} of each other.
 *
 * @author Henning Schulz
 *
 */
public abstract class ListInput extends AbstractIdpaElement implements Input {

	@JsonProperty(value = "associated")
	@JsonInclude(Include.NON_EMPTY)
	@JsonIdentityReference(alwaysAsId = true)
	private List<ListInput> associated;

	/**
	 * Returns the associated inputs.
	 *
	 * @return The associated inputs.
	 */
	public List<ListInput> getAssociated() {
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
	public void setAssociated(List<ListInput> associated) {
		this.associated = associated;
	}

}
