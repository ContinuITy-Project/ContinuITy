package org.continuity.commons.utils;

/**
 * Utility class for String manipulation.
 *
 * @author Henning Schulz
 *
 */
public class StringUtils {

	private StringUtils() {
	}

	/**
	 * Formats the specified strings into a string that can be used as an ID in, e.g., a yaml file.
	 * Uses {@code _} as delimiter.
	 *
	 * @param shorten
	 *            Whether the ID should be shortened. That is, from each element of the strings only
	 *            the first char will be used - except for the last element.
	 * @param strings
	 *            The strings to be formatted as ID. Will be appended to each other.
	 * @return A string that can be used as an ID.
	 */
	public static String formatAsId(boolean shorten, String... strings) {
		StringBuilder builder = new StringBuilder();

		boolean first = true;

		for (String s : strings) {
			if (first) {
				first = false;
			} else {
				builder.append("_");
			}

			appendAsIdPart(s, builder, shorten);
		}

		return builder.toString();
	}

	private static String appendAsIdPart(String string, StringBuilder builder, boolean shorten) {
		String[] tokens = string.split("[^a-zA-Z0-9_]");

		boolean first = true;

		for (int i = 0; i < (tokens.length - 1); i++) {
			if (tokens[i].length() > 0) {
				if (!first) {
					builder.append("_");
				}

				if (shorten) {
					builder.append(tokens[i].substring(0, 1));
				} else {
					builder.append(tokens[i]);
				}

				first = false;
			}
		}

		if (tokens[tokens.length - 1].length() > 0) {
			if (!first) {
				builder.append("_");
			}

			builder.append(tokens[tokens.length - 1].replaceAll("[^a-zA-Z0-9_]", "_"));
		}

		return builder.toString();
	}

}
