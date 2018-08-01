package org.continuity.api.entities.report;

import org.continuity.api.entities.links.LinkExchangeModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TaskReport {

	private String taskId;

	private boolean successful;

	@JsonInclude(Include.NON_NULL)
	private LinkExchangeModel result;

	@JsonInclude(Include.NON_NULL)
	private TaskError error;

	public TaskReport(String taskId, boolean successful, LinkExchangeModel result, TaskError error) {
		this.taskId = taskId;
		this.successful = successful;
		this.result = result;
		this.error = error;
	}

	public static TaskReport successful(String taskId, LinkExchangeModel result) {
		return new TaskReport(taskId, true, result, null);
	}

	public static TaskReport error(String taskId, TaskError error) {
		return new TaskReport(taskId, false, null, error);
	}


	public TaskReport() {
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public LinkExchangeModel getResult() {
		return result;
	}

	public void setResult(LinkExchangeModel result) {
		this.result = result;
	}

	public TaskError getError() {
		return error;
	}

	public void setError(TaskError error) {
		this.error = error;
	}

}
