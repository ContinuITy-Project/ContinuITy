/**
 */
package org.continuity.workload.dsl.system;

import java.util.List;

import org.continuity.workload.dsl.ContinuityModelElement;

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
@JsonSubTypes({ @Type(value = HttpInterface.class, name = "http") })
public interface ServiceInterface<P extends Parameter> extends ContinuityModelElement {

	/**
	 * Returns representations of the parameters of the interface.
	 *
	 * @return The parameters.
	 */
	public List<P> getParameters();

}
