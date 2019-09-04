package org.continuity.commons.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *
 * @author Henning Schulz
 *
 */
public class DateUtils {

	private DateUtils() {
	}

	/**
	 * Gets the epoch milliseconds from a {@link LocalDateTime} using the system default time zone.
	 *
	 * @param datetime
	 *            The date.
	 * @return The milliseconds.
	 */
	public static long toEpochMillis(LocalDateTime datetime) {
		return datetime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

}
