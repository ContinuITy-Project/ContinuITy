package org.continuity.api.entities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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

	public static <T> Map<Date, T> parseKeys(Map<String, T> map) throws ParseException {
		Map<Date, T> dateMap = new HashMap<>();

		for (Entry<String, T> entry : map.entrySet()) {
			Date date = ApiFormats.DATE_FORMAT.parse(entry.getKey());
			dateMap.put(date, entry.getValue());
		}

		return dateMap;
	}

}
