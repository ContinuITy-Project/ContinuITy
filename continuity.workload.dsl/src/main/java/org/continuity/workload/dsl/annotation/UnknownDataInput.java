package org.continuity.workload.dsl.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Can be used to specify any kind of data input. The data has to be specified in the annotation
 * extension.
 *
 * @author Henning Schulz
 *
 */
public class UnknownDataInput extends DataInput {

	@JsonProperty(value = "type", required = true)
	private String type;

	/**
	 * Gets {@link #type}.
	 *
	 * @return {@link #type}
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets {@link #type}.
	 *
	 * @param type
	 *            New value for {@link #type}
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (id: ");
		result.append(getId());
		result.append(", data: UNKNOWN");
		result.append(')');
		return result.toString();
	}

}
