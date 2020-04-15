package org.continuity.cobra.entities;

import java.util.Map;

import org.continuity.lctl.elements.TypedProperties;

/**
 *
 * @author Henning Schulz
 *
 */
public class TypeAndProperties {

	private String type;

	private Map<String, Object> properties;

	public TypeAndProperties() {
	}

	public TypeAndProperties(String type, Map<String, Object> properties) {
		this.type = type;
		this.properties = properties;
	}

	public static TypeAndProperties fromTypedProperties(TypedProperties props) {
		return new TypeAndProperties(props.getType(), props.getProperties());
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

}
