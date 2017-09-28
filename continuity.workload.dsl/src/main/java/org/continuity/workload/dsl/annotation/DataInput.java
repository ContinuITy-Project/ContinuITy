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
public class DataInput implements Input {

	private String name;

	private List<String> data;

	private List<DataInput> associated;

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
	 * Returns the held data.
	 *
	 * @return The data.
	 */
	public List<String> getData() {
		if (data == null) {
			data = new ArrayList<>();
		}

		return this.data;
	}

	/**
	 * Sets the data to be held.
	 *
	 * @param data
	 *            The data.
	 */
	public void setData(List<String> data) {
		this.data = data;
	}

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

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(", data: ");
		result.append(data);
		result.append(')');
		return result.toString();
	}

} // DataInput
