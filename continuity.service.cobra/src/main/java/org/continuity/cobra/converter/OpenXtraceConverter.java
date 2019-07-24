package org.continuity.cobra.converter;

import java.util.List;

import org.spec.research.open.xtrace.api.core.Trace;

/**
 * Common interface for converters from a certain data type to OPEN.xtrace.
 *
 * @author Henning Schulz
 *
 * @param <T>
 */
public interface OpenXtraceConverter<T> {

	public static final long MILLIS_TO_NANOS = 1000000;

	public static final long MICROS_TO_NANOS = 1000;

	/**
	 * Converts a list of data to OPEN.xtrace.
	 *
	 * @param data
	 *            The data to be converted.
	 * @return The OPEN.xtrace (list of {@link Trace}).
	 */
	List<Trace> convert(List<T> data);

}
