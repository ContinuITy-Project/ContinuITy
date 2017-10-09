/**
 */
package org.continuity.workload.dsl.system;

import java.util.ArrayList;
import java.util.List;

import org.continuity.workload.dsl.AbstractContinuityModelElement;

/**
 * Represents a system consisting of interfaces that can be called.
 *
 * @author Henning Schulz
 *
 */
public class TargetSystem extends AbstractContinuityModelElement {

	private List<ServiceInterface> interfaces;

	/**
	 * Returns the interface representations of the represented system.
	 *
	 * @return the interface representations of the represented system
	 */
	public List<ServiceInterface> getInterfaces() {
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
	public void setInterfaces(List<ServiceInterface> interfaces) {
		this.interfaces = interfaces;
	}

	public void addInterface(ServiceInterface sInterface) {
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
