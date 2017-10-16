/**
 */
package org.continuity.workload.dsl.annotation;

import org.continuity.workload.dsl.ContinuityModelElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Represents an input to a parameter.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
@JsonSubTypes({ @Type(value = DirectDataInput.class, name = "direct"), @Type(value = CsvInput.class, name = "csv"), @Type(value = ExtractedInput.class, name = "extracted"),
		@Type(value = CustomDataInput.class, name = "custom") })
public interface Input extends ContinuityModelElement {

}
