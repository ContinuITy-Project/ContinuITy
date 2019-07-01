package org.continuity.api.entities.artifact;

import java.util.List;

import org.continuity.idpa.VersionOrTimestamp;

public class SessionsBundlePack {

	private VersionOrTimestamp version;

	private List<SessionsBundle> sessionsBundles;

	public SessionsBundlePack(VersionOrTimestamp version, List<SessionsBundle> sessionsBundles) {
		super();
		this.version = version;
		this.sessionsBundles = sessionsBundles;
	}

	public SessionsBundlePack() {

	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public void setVersion(VersionOrTimestamp version) {
		this.version = version;
	}

	public List<SessionsBundle> getSessionsBundles() {
		return sessionsBundles;
	}

	public void setSessionsBundles(List<SessionsBundle> sessionsBundles) {
		this.sessionsBundles = sessionsBundles;
	}

}
