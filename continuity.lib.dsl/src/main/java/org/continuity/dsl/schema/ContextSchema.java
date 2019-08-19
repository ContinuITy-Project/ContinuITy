package org.continuity.dsl.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.continuity.dsl.timeseries.ContextRecord;
import org.continuity.dsl.timeseries.NumericVariable;
import org.continuity.dsl.timeseries.StringVariable;
import org.continuity.dsl.validation.ContextValidityReport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes which kind of variables are allowed in contexts.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "ignore-by-default", "auto-detect", "variables" })
public class ContextSchema {

	@JsonProperty("ignore-by-default")
	@JsonInclude(Include.NON_ABSENT)
	private Optional<IgnoreByDefaultValue> ignoreByDefault = Optional.empty();

	@JsonProperty("auto-detect")
	@JsonInclude(Include.NON_ABSENT)
	private boolean autoDetect = true;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, VariableSchema> variables = new HashMap<>();

	/**
	 * Optional property whether all context variables should be ignored for forecasting. Default is
	 * {@link IgnoreByDefaultValue#ONLY_NEW};
	 *
	 * @return
	 */
	public Optional<IgnoreByDefaultValue> getIgnoreByDefault() {
		return ignoreByDefault;
	}

	@JsonIgnore
	public IgnoreByDefaultValue ignoreByDefault() {
		return ignoreByDefault.orElse(IgnoreByDefaultValue.ONLY_NEW);
	}

	public void setIgnoreByDefault(Optional<IgnoreByDefaultValue> ignoreByDefault) {
		this.ignoreByDefault = ignoreByDefault;
	}

	public boolean getAutoDetect() {
		return autoDetect;
	}

	public void setAutoDetect(boolean autoDetect) {
		this.autoDetect = autoDetect;
	}

	public Map<String, VariableSchema> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, VariableSchema> variables) {
		this.variables = variables;
	}

	public ContextValidityReport validate(ContextRecord record) {
		ContextValidityReport report = new ContextValidityReport();

		if (record.getNumeric() != null) {
			for (NumericVariable var : record.getNumeric()) {
				validate(var.getName(), VariableType.NUMERIC, report);
			}
		}

		if (record.getString() != null) {
			for (StringVariable var : record.getString()) {
				validate(var.getName(), VariableType.STRING, report);
			}
		}

		if (record.getBoolean() != null) {
			for (String var : record.getBoolean()) {
				validate(var, VariableType.BOOLEAN, report);
			}
		}

		return report;
	}

	private void validate(String name, VariableType type, ContextValidityReport report) {
		VariableSchema varSchema = getVariables().get(name);

		if (varSchema == null) {
			if (autoDetect) {
				report.reportNewlyAdded(name, type);
			} else {
				report.reportUnknown(name, type);
			}
		} else if (varSchema.getType() != type) {
			report.reportWrongType(name, varSchema.getType(), type);
		}
	}

}
