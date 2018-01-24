/**
 */
package org.continuity.annotation.dsl.system;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.continuity.annotation.dsl.AbstractContinuityModelElement;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a system consisting of interfaces that can be called.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "timestamp", "interfaces" })
public class SystemModel extends AbstractContinuityModelElement {

	/**
	 * Default value is the date when the object was created.
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
	private Date timestamp = new Date();

	private List<ServiceInterface<?>> interfaces;

	/**
	 * Returns the interface representations of the represented system.
	 *
	 * @return the interface representations of the represented system
	 */
	public List<ServiceInterface<?>> getInterfaces() {
		if (interfaces == null) {
			interfaces = new ArrayList<>();
		}
		return interfaces;
	}

	/**
	 * Sets the interface representations of the represented system.
	 *
	 * @param interfaces
	 *            The interface representations of the represented system.
	 */
	public void setInterfaces(List<ServiceInterface<?>> interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * Gets the date at which the system is represented.
	 *
	 * @return The timestamp.
	 */
	public Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the date at which the system is represented.
	 *
	 * @param timestamp
	 *            The timestamp.
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void addInterface(ServiceInterface<?> sInterface) {
		getInterfaces().add(sInterface);
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
