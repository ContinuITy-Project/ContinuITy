package org.continuity.api.entities.config.cobra;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Henning Schulz
 *
 */
public class TracesConfiguration {

	@JsonProperty("discard-unmapped")
	private boolean discardUmapped = false;

	@JsonProperty("log-unmapped")
	private boolean logUnmapped = true;

	/**
	 *
	 * @return Whether traces that could not be mapped to any endpoint should be discarded
	 *         ({@code true}) or stored anyway ({@code false}). Defaults to {@code false}.
	 */
	public boolean isDiscardUmapped() {
		return discardUmapped;
	}

	public void setDiscardUmapped(boolean discardUmapped) {
		this.discardUmapped = discardUmapped;
	}

	/**
	 *
	 * @return Whether traces that could not be mapped should be logged to a file.
	 */
	public boolean isLogUnmapped() {
		return logUnmapped;
	}

	public void setLogUnmapped(boolean logUnmapped) {
		this.logUnmapped = logUnmapped;
	}

}
