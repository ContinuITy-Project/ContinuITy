package org.continuity.api.entities.artifact;

/**
 * Represents a simplified session.
 * @author Alper Hidiroglu
 *
 */
public class SimplifiedSession {
	
	private String id;
	private long startTime;
	private long endTime;
	
	/**
	 * Constructor.
	 * @param id
	 * @param startTime
	 * @param endTime
	 */
	public SimplifiedSession(String id, long startTime, long endTime) {
		this.id = id;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public SimplifiedSession() {
		
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
}
