/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.ContinuityModelElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Represents an input to a parameter.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = DirectDataInput.class, name = "direct"), @Type(value = CsvInput.class, name = "csv"), @Type(value = ExtractedInput.class, name = "extracted"),
	@Type(value = UnknownDataInput.class, name = "unknown") })
public interface Input extends ContinuityModelElement {

}
