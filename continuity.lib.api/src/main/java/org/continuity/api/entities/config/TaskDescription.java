package org.continuity.api.entities.config;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.dsl.description.ForecastInput;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TaskDescription {

	/**
	 * recipeId.subtaskId
	 */
	private String taskId;

	private String tag;

	private LinkExchangeModel source;

	@JsonInclude(Include.NON_NULL)
	private PropertySpecification properties;

	@JsonProperty("long-term-use")
	private boolean longTermUse;

	@JsonProperty("modularization-options")
	private ModularizationOptions modularizationOptions;
	
	private ForecastInput forecastInput;

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public LinkExchangeModel getSource() {
		return source;
	}

	public void setSource(LinkExchangeModel source) {
		this.source = source;
	}

	public PropertySpecification getProperties() {
		return properties;
	}

	public void setProperties(PropertySpecification properties) {
		this.properties = properties;
	}

	public boolean isLongTermUse() {
		return longTermUse;
	}

	public void setLongTermUse(boolean longTermUse) {
		this.longTermUse = longTermUse;
	}

	public ModularizationOptions getModularizationOptions() {
		return modularizationOptions;
	}

	public void setModularizationOptions(ModularizationOptions modularizationOptions) {
		this.modularizationOptions = modularizationOptions;
	}
	
	public ForecastInput getForecastInput() {
		return forecastInput;
	}

	public void setForecastInput(ForecastInput forecastInput) {
		this.forecastInput = forecastInput;
	}

}
