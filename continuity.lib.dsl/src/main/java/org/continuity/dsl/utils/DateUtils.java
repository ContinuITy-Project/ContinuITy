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
	 * Gets the epoch milliseconds from a {@link LocalDateTime}.
	 *
	 * @param datetime
	 *            The date.
	 * @param timeZone
	 *            The time zone.
	 * @return The milliseconds.
	 */
	public static long toEpochMillis(LocalDateTime datetime, ZoneId timeZone) {
		return datetime.atZone(timeZone).toInstant().toEpochMilli();
	}

	/**
	 * Gets a {@link LocalDateTime} for the given epoch milliseconds.
	 *
	 * @param millis
	 *            The epoch milliseconds.
	 * @param timeZone
	 *            The time zone.
	 * @return The date.
	 */
	public static LocalDateTime fromEpochMillis(long millis, ZoneId timeZone) {
		return Instant.ofEpochMilli(millis).atZone(timeZone).toLocalDateTime();
	}

}
