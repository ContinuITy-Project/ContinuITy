/**
 */
package org.continuity.workload.dsl.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a system consisting of interfaces that can be called.
 *
 * @author Henning Schulz
 *
 */
public class TargetSystem {

	private static final String NAME_DEFAULT = "UNKNOWN";

	private String name = NAME_DEFAULT;

	private List<ServiceInterface> interfaces;

	/**
	 * Returns the name of the represented system.
	 *
	 * @return The name of the represented system.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the represented system.
	 *
	 * @param name
	 *            The name of the represented system.
	 */
	public void setName(String name) {
		this.name = name;
	}

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
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

}
