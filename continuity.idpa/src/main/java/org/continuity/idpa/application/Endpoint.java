/**
 */
package org.continuity.idpa.application;

import java.util.List;

import org.continuity.idpa.IdpaElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Representation of an interface of a system that can be called.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
@JsonSubTypes({ @Type(value = HttpEndpoint.class, name = "http") })
public interface Endpoint<P extends Parameter> extends IdpaElement {

	@SuppressWarnings("unchecked")
	public static final Class<Endpoint<?>> GENERIC_TYPE = (Class<Endpoint<?>>) (Class<?>) Endpoint.class;

	/**
	 * Returns representations of the parameters of the interface.
	 *
	 * @return The parameters.
	 */
	public List<P> getParameters();

	/**
	 * Adds a parameter.
	 *
	 * @param parameter
	 *            The parameter to be added.
	 */
	public void addParameter(P parameter);

	/**
	 * Returns a list of property names that differ between this interface and the specified one.
	 *
	 * @param other
	 *            Interface to compare to.
	 * @return The differing properties.
	 */
	public List<String> getDifferingProperties(Endpoint<?> other);

	/**
	 * Sets the specified property by cloning it from another interface.
	 *
	 * @param propertyName
	 *            The name of the property to be cloned.
	 * @param other
	 *            The interface to clone the property from.
	 * @return {@code true}, if the property was successfully cloned or {@code false} if cloning was
	 *         impossible (e.g., due to a wrong type of the other interface).
	 */
	public boolean clonePropertyFrom(String propertyName, Endpoint<?> other);

}
