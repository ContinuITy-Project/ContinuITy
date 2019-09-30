package org.continuity.api.entities.config.cobra;

import java.time.Duration;

import org.continuity.api.entities.config.cobra.CobraConfiguration.DurationToStringConverter;
import org.continuity.api.entities.config.cobra.CobraConfiguration.StringToDurationConverter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
public class TracesConfiguration {

	@JsonProperty("map-to-idpa")
	private boolean mapToIdpa = true;

	@JsonProperty("discard-unmapped")
	private boolean discardUmapped = false;

	@JsonProperty("log-unmapped")
	private boolean logUnmapped = true;

	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration retention = Duration.ofDays(3650);

	/**
	 * Whether the uploaded traces should be mapped to IDPA endpoints. Setting it to {@code false}
	 * disallows for tailoring and will use the provided business transactions as endpoint names.
	 * Defaults to {@code true}.
	 *
	 * @return
	 */
	public boolean isMapToIdpa() {
		return mapToIdpa;
	}

	public void setMapToIdpa(boolean mapToIdpa) {
		this.mapToIdpa = mapToIdpa;
	}

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

	/**
	 *
	 * @return The duration after which the stored traces will be removed from the database.
	 */
	public Duration getRetention() {
		return retention;
	}

	public void setRetention(Duration retention) {
		this.retention = retention;
	}

}
