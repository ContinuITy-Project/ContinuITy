package org.continuity.api.entities.artifact;

import java.util.List;

/**
 * 
 * @author Alper Hidiroglu
 *
 */
public class SessionsBundle {
	
	private int behaviorId;
	private List<SimplifiedSession> sessions;
	
	public SessionsBundle(int behaviorId, List<SimplifiedSession> sessions) {
		this.behaviorId = behaviorId;
		this.sessions = sessions;
	}
	
	public SessionsBundle() {
		
	}

	public int getBehaviorId() {
		return behaviorId;
	}
	public void setBehaviorId(int behaviorId) {
		this.behaviorId = behaviorId;
	}
	public List<SimplifiedSession> getSessions() {
		return sessions;
	}
	public void setSessions(List<SimplifiedSession> sessions) {
		this.sessions = sessions;
	}
}
