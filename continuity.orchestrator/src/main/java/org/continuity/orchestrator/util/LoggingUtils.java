package org.continuity.orchestrator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUtils.class);

	private LoggingUtils() {
	}

	/**
	 * Format a string in the form {@code O[orderId]} to be used as a prefix for logging.
	 *
	 * @param orderId
	 * @return The formatted prefix.
	 */
	public static String formatPrefix(String orderId) {
		return formatPrefix(orderId, null, null);
	}

	/**
	 * Format a string in the form {@code O[orderId] R[recipeId]} to be used as a prefix for
	 * logging.
	 *
	 * @param orderId
	 * @param recipeId
	 * @return The formatted prefix.
	 */
	public static String formatPrefix(String orderId, String recipeId) {
		return formatPrefix(orderId, recipeId, null);
	}

	/**
	 * Format a string in the form {@code O[orderId] R[recipeId] T[taskId]} to be used as a prefix
	 * for logging.
	 *
	 * @param orderId
	 * @param recipeId
	 * @param taskId
	 * @return The formatted prefix.
	 */
	public static String formatPrefix(String orderId, String recipeId, String taskId) {
		StringBuilder builder = new StringBuilder();

		builder.append(String.format("O[%12.12s]", truncateLeft(orderId, 12)));

		if (recipeId != null) {
			builder.append(String.format(" R[%12.12s]", truncateLeft(recipeId, 12)));

			if (taskId != null) {
				int ptIndex = taskId.lastIndexOf(".");
				String taskPart = taskId.substring(ptIndex + 1);
				String recipePart = taskId.substring(0, ptIndex);

				builder.append(String.format(" T[%-6.6s.%12.12s]", truncateLeft(recipePart, 6), taskPart));
			}
		}

		return builder.toString();

	}

	private static String truncateLeft(String s, int n) {
		int l = s.length();

		if (l > n) {
			return s.substring(l - n);
		} else {
			return s;
		}
	}

}
