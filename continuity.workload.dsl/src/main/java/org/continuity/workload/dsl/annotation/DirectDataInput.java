package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Directly holds the data.
 *
 * @author Henning Schulz
 *
 */
public class DirectDataInput extends DataInput {

	@JsonProperty(value = "data")
	private List<String> data;

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

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (id: ");
		result.append(getId());
		result.append(", data: ");
		result.append(data);
		result.append(')');
		return result.toString();
	}

}
