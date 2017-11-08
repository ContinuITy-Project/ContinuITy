package org.continuity.annotation.dsl.ann;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Directly holds the data.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "data", "associated" })
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
