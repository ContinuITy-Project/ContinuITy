package org.continuity.idpa.annotation;

import java.text.SimpleDateFormat;

import org.continuity.idpa.AbstractIdpaElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Generates the current date plus an offset. The date format is the same as
 * {@link SimpleDateFormat}. The offset format is the ISO 8601 PnDTnHnMn format. Note that for some
 * load drivers (such as JMeter), an empty date format produces the time stamp in millis.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "format", "offset" })
public class DatetimeInput extends AbstractIdpaElement implements Input {

	private String format;

	@JsonProperty(required = false)
	private String offset;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
	}

}
