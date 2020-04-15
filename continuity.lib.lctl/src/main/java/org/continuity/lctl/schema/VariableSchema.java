package org.continuity.lctl.schema;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes an individual variable in the context.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "type", "ignore-by-default" })
public class VariableSchema {

	private VariableType type;

	@JsonProperty("ignore-by-default")
	@JsonInclude(Include.NON_ABSENT)
	private Optional<Boolean> ignoreByDefault = Optional.empty();

	public VariableSchema() {
	}

	public VariableSchema(VariableType type) {
		this.type = type;
	}

	public VariableSchema(VariableType type, boolean ignoreByDefault) {
		this.type = type;
		this.ignoreByDefault = Optional.of(ignoreByDefault);
	}

	public VariableType getType() {
		return type;
	}

	public void setType(VariableType type) {
		this.type = type;
	}

	public Optional<Boolean> getIgnoreByDefault() {
		return ignoreByDefault;
	}

	public void setIgnoreByDefault(Optional<Boolean> ignoreByDefault) {
		this.ignoreByDefault = ignoreByDefault;
	}

}
