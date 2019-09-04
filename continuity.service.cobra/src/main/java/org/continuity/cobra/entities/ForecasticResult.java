package org.continuity.cobra.entities;

import java.util.List;

import org.continuity.api.entities.artifact.ForecastIntensityRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecasticResult {

	private List<ForecastIntensityRecord> intensities;

	public List<ForecastIntensityRecord> getIntensities() {
		return intensities;
	}

	public void setIntensities(List<ForecastIntensityRecord> intensities) {
		this.intensities = intensities;
	}

}
