package org.continuity.api.entities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * @author Henning Schulz
 *
 */
public class ApiFormats {

	public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH-mm-ss-SSSX";

	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);

	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private ApiFormats() {
	}

}
