package org.continuity.dsl.utils;

import java.time.Instant;
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

	/**
	 * Gets a {@link LocalDateTime} for the given epoch milliseconds using the system default time
	 * zone.
	 * 
	 * @param millis
	 *            The epoch milliseconds.
	 * @return The date.
	 */
	public static LocalDateTime fromEpochMillis(long millis) {
		return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

}
