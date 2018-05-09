/**
 */
package org.continuity.idpa.application;

import java.util.List;

import org.continuity.idpa.IdpaElement;

/**
 * Represents a parameter of a {@link Endpoint}
 *
 * @author Henning Schulz
 *
 */
public interface Parameter extends IdpaElement, Comparable<Parameter> {

	public List<String> getDifferingProperties(Parameter other);

}
