package org.continuity.api.entities.config;

import org.continuity.api.entities.links.MeasurementDataType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Used to specify the location and properties of externally stored measurement data.
 *
 * @author Henning Schulz
 *
 */
public class MeasurementDataSpec {

	private MeasurementDataType type;

	private String link;

	@JsonInclude(Include.NON_NULL)
	private String pattern;

	public MeasurementDataType getType() {
		return type;
	}

	public void setType(MeasurementDataType type) {
		this.type = type;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Gets the pattern (regular expression) used to parse access logs
	 *
	 * @return
	 */
	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

}
