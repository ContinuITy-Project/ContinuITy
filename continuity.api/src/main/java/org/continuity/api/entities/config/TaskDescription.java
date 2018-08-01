package org.continuity.api.entities.config;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TaskDescription {

	/**
	 * recipeId.subtaskId
	 */
	private String taskId;

	private String tag;

	private LinkExchangeModel source;

	@JsonInclude(Include.NON_NULL)
	private PropertySpecification properties;

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

}
