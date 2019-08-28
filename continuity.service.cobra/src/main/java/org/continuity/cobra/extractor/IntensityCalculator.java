package org.continuity.cobra.extractor;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.dsl.timeseries.IntensityRecord;

/**
 * Calculates the session intensity over time.
 *
 * @author Henning Schulz
 *
 */
public class IntensityCalculator {

	private final String group;

	private final long resolutionMicros;

	private final long startMicros;

	private final long endMicros;

	private final long leftShiftMicros;

	/**
	 * Constructor.
	 *
	 * @param group
	 *            The behavior group ID.
	 * @param resolutionMicros
	 *            The resolution of the intensities in microseconds. {@code endMicros - startMicros}
	 *            needs to be a multiple of {@code resolutionMicros}.
	 * @param startMicros
	 *            The start timestamp of the intensity range to be considered in microseconds.
	 * @param endMicros
	 *            The start timestamp of the intensity range to be considered in microseconds.
	 * @param leftShiftMicros
	 *            A correction term such that
	 *            {@code (startMicros + leftShiftMicros) % resolutionMicros == 0}.
	 */
	public IntensityCalculator(String group, long resolutionMicros, long startMicros, long endMicros, long leftShiftMicros) {
		this.group = group;
		this.resolutionMicros = resolutionMicros;
		this.startMicros = startMicros;
		this.endMicros = endMicros;
		this.leftShiftMicros = leftShiftMicros;
	}

	/**
	 * Calculates the intensity over time.
	 *
	 * @param sessions
	 *            The sessions that are active during the time range under consideration.
	 * @return The intensity as list of {@link IntensityRecord}.
	 */
	public List<IntensityRecord> calculate(List<Session> sessions) {
		return sessions.stream().map(this::extractTimestamps).flatMap(this::split).filter(this::filter).map(this::convertToDuration)
				.collect(collectingAndThen(groupingBy(Pair::getKey), this::summarizeDurations));
	}

	/**
	 * Extracts the start and end timestamp.
	 *
	 * @param session
	 * @return {@code Pair(start, end)}.
	 */
	private Pair<Long, Long> extractTimestamps(Session session) {
		return Pair.of(session.getStartMicros(), session.getEndMicros());
	}

	/**
	 * Splits the sessions according to the resolution.
	 *
	 * @param session
	 * @return {@code List<Pair(start, end)>}.
	 */
	private Stream<Pair<Long, Long>> split(Pair<Long, Long> session) {
		long intervalStart = floorTimestamp(session.getLeft());
		long steps = (long) Math.ceil((double) (session.getRight() - intervalStart) / resolutionMicros);

		return Stream.iterate(intervalStart, i -> i + resolutionMicros).limit(steps).map(start -> Pair.of(Math.max(session.getLeft(), start), Math.min(session.getRight(), start + resolutionMicros)));
	}

	/**
	 * Filters all sessions that are inside the calculation range.
	 *
	 * @param session
	 * @return {@code true} if the session is inside {@code [startMicros, endMicros]}.
	 */
	private boolean filter(Pair<Long, Long> session) {
		return (session.getLeft() >= startMicros) && (session.getRight() <= endMicros);
	}

	/**
	 * Converts the start-end sessions into durations.
	 *
	 * @param session
	 * @return {@code Pair(floor_start, duration)}.
	 */
	private Pair<Long, Long> convertToDuration(Pair<Long, Long> session) {
		return Pair.of(floorTimestamp(session.getLeft()), session.getRight() - session.getLeft());
	}

	/**
	 * Summarizes the durations grouped by floor timestamp.
	 *
	 * @param groupedDurations
	 * @return The list of intensity records.
	 */
	private List<IntensityRecord> summarizeDurations(Map<Long, List<Pair<Long, Long>>> groupedDurations) {
		return groupedDurations.entrySet().stream().map(this::calculateIntensity).map(this::toRecord).collect(toList());
	}

	/**
	 * Calculates the intensity based on the durations of the timestamp.
	 *
	 * @param durations
	 * @return {@code Pair(floor_start, intensity)}.
	 */
	private Pair<Long, Long> calculateIntensity(Entry<Long, List<Pair<Long, Long>>> durations) {
		double sum = durations.getValue().stream().mapToLong(Pair::getRight).sum();
		return Pair.of(durations.getKey(), Math.round(sum / resolutionMicros));
	}

	/**
	 * Transforms the intensity pair to a record.
	 *
	 * @param intensity
	 * @return The intensity as record.
	 */
	private IntensityRecord toRecord(Pair<Long, Long> intensity) {
		IntensityRecord record = new IntensityRecord();
		record.setTimestamp(intensity.getKey() / 1000); // timestamp is in millis
		record.setIntensity(Collections.singletonMap(group, intensity.getValue()));
		return record;
	}

	/**
	 * Calculates the largest timestamp that is a multiple of {@link #resolutionMicros} and is
	 * smaller than the passed timestamp.
	 *
	 * @param timestamp
	 * @return The floor timestamp.
	 */
	private long floorTimestamp(long timestamp) {
		return (Math.floorDiv(timestamp + leftShiftMicros, resolutionMicros) * resolutionMicros) - leftShiftMicros;
	}

}
