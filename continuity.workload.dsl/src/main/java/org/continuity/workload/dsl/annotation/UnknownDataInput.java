package org.continuity.workload.dsl.annotation;

/**
 * Can be used to specify any kind of data input. The data has to be specified in the annotation
 * extension.
 *
 * @author Henning Schulz
 *
 */
public class UnknownDataInput extends DataInput {

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
