package org.continuity.cobra.entities;

/**
 * Defines the current status of trace processing - e.g., whether traces are processed or whether
 * all uploads are rejected.
 *
 * @author Henning Schulz
 *
 */
public class TraceProcessingStatus {

	private boolean active = true;

	/**
	 * If {@code true}, traces are processed. If {@code false}, all newly uploaded traces are
	 * rejected and no ones are processed anymore.
	 * 
	 * @return
	 */
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
