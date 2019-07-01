package org.continuity.api.entities.artifact;

import org.continuity.idpa.VersionOrTimestamp;

public class SessionLogs {

	private VersionOrTimestamp version;

	private String logs;

	public SessionLogs(VersionOrTimestamp version, String logs) {
		this.version = version;
		this.logs = logs;
	}

	public SessionLogs() {
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public String getLogs() {
		return logs;
	}

	public void setLogs(String logs) {
		this.logs = logs;
	}
}
