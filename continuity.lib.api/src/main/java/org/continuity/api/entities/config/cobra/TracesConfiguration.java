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

}
