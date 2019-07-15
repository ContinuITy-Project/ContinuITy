package org.continuity.api.entities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

	public static String formatOrNull(Date date) {
		if (date == null) {
			return null;
		} else {
			return DATE_FORMAT.format(date);
		}
	}

	public static Date parseOrNull(String source) throws ParseException {
		if (source == null) {
			return null;
		} else {
			return DATE_FORMAT.parse(source);
		}
	}

}
