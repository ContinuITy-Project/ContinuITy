/**
 */
package org.continuity.idpa.annotation;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.json.JsonInput;

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
@JsonSubTypes({ @Type(value = DirectListInput.class, name = "direct"), @Type(value = CsvInput.class, name = "csv"), @Type(value = ExtractedInput.class, name = "extracted"),
		@Type(value = CounterInput.class, name = "counter"), @Type(value = JsonInput.class, name = "json"), @Type(value = RandomNumberInput.class, name = "randnum"),
		@Type(value = RandomStringInput.class, name = "randstring"), @Type(value = DatetimeInput.class, name = "datetime"), @Type(value = CombinedInput.class, name = "combined") })
public interface Input extends IdpaElement {

}
