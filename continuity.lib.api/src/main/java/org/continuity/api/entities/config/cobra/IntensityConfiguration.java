package org.continuity.api.entities.config.cobra;

import java.time.Duration;

import org.continuity.api.entities.config.cobra.CobraConfiguration.DurationToStringConverter;
import org.continuity.api.entities.config.cobra.CobraConfiguration.StringToDurationConverter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
public class IntensityConfiguration {

	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration resolution = Duration.ofMinutes(1);

	public Duration getResolution() {
		return resolution;
	}

	public void setResolution(Duration resolution) {
		this.resolution = resolution;
	}

}
