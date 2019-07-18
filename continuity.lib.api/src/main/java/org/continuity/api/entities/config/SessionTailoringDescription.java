package org.continuity.api.entities.config;

import java.util.List;

import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes how to generate a tailored behavior model from sessions.
 *
 * @author Henning Schulz
 *
 */
public class SessionTailoringDescription {

	@JsonProperty("app-id")
	private AppId aid;

	private VersionOrTimestamp version;

	@JsonProperty("root-endpoint")
	private String rootEndpoint;

	@JsonProperty("include-pre-post-processing")
	private boolean includePrePostProcessing;

	private List<String> tailoring;

	@JsonProperty("session-ids")
	private List<String> sessionIds;

	public AppId getAid() {
		return aid;
	}

	public void setAid(AppId aid) {
		this.aid = aid;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public String getRootEndpoint() {
		return rootEndpoint;
	}

	public void setRootEndpoint(String rootEndpoint) {
		this.rootEndpoint = rootEndpoint;
	}

	public boolean isIncludePrePostProcessing() {
		return includePrePostProcessing;
	}

	public void setIncludePrePostProcessing(boolean includePrePostProcessing) {
		this.includePrePostProcessing = includePrePostProcessing;
	}

	public List<String> getTailoring() {
		return tailoring;
	}

	public void setTailoring(List<String> tailoring) {
		this.tailoring = tailoring;
	}

	public List<String> getSessionIds() {
		return sessionIds;
	}

	public void setSessionIds(List<String> sessionIds) {
		this.sessionIds = sessionIds;
	}

}
