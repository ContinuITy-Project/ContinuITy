package org.continuity.api.entities.artifact;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class SessionsBundlePack {
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH-mm-ss-SSSX")
	private Date timestamp;

	private List<SessionsBundle> sessionsBundles;

	public SessionsBundlePack(Date timestamp, List<SessionsBundle> sessionsBundles) {
		super();
		this.timestamp = timestamp;
		this.sessionsBundles = sessionsBundles;
	}
	
	public SessionsBundlePack() {
		
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public List<SessionsBundle> getSessionsBundles() {
		return sessionsBundles;
	}

	public void setSessionsBundles(List<SessionsBundle> sessionsBundles) {
		this.sessionsBundles = sessionsBundles;
	}

}
