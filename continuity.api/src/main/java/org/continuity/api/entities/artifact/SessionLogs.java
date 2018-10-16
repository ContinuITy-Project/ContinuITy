package org.continuity.api.entities.artifact;

import java.util.Date;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionLogs {

	@JsonProperty("data-timestamp")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH-mm-ss-SSSX")
	private Date dataTimestamp;

	private String logs;

	public SessionLogs(Date dataTimestamp, String logs) {
		this.dataTimestamp = dataTimestamp;
		this.logs = logs;
	}

	public SessionLogs() {
	}

	public Date getDataTimestamp() {
		return dataTimestamp;
	}

	public void setDataTimestamp(Date dataTimestamp) {
		this.dataTimestamp = dataTimestamp;
	}

	public String getLogs() {
		return logs;
	}

	public void setLogs(String logs) {
		this.logs = logs;
	}
}
