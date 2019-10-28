package org.continuity.cobra.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
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

	private long startMicros;

	private final long endMicros;

	private final long leftShiftMicros;

	private final Map<Long, List<Long>> collectedDurations = new TreeMap<>();

	private final List<IntensityRecord> records = new ArrayList<>();

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
	 * Adds a new set of sessions from which the intensities are calculated. Can be called multiple
	 * times for multiple chunks of sessions. The chunks must be passed in chronological order
	 * according to the start-micros.
	 *
	 * @param sessions
	 *            (A chunk of) the sessions that are active during the time range under
	 *            consideration.
	 */
	public void addSessions(List<Session> sessions) {
		long nextStart = floorTimestamp(sessions.stream().mapToLong(Session::getStartMicros).max().orElse(this.startMicros));

		collectDurations(sessions);
		transformDurations(nextStart);
	}

	/**
	 * Gets the calculated intensity records, finishing the remaining durations.
	 *
	 * @return
	 */
	public List<IntensityRecord> getRecords() {
		transformDurations(Long.MAX_VALUE);

		return records;
	}

	/**
	 * Collects the durations split according to the resolution.
	 *
	 * @param sessions
	 */
	private void collectDurations(List<Session> sessions) {
		sessions.stream().map(this::extractTimestamps).flatMap(this::split).filter(this::filter).map(this::convertToDuration).forEach(this::collectDuration);
	}

	/**
	 * Transforms the durations into intensity records.
	 *
	 * @param until
	 *            The timestamp in microseconds until which the durations should be transformed
	 *            (exclusively).
	 */
	private void transformDurations(long until) {
		Iterator<Entry<Long, List<Long>>> iterator = collectedDurations.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<Long, List<Long>> entry = iterator.next();

			if (entry.getKey() >= until) {
				break;
			}

			records.add(calculateIntensity(entry.getKey(), entry.getValue()));
			iterator.remove();
		}
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
	 * Stores the duration to the {@link #collectedDurations} map.
	 *
	 * @param session
	 *            {@code Pair(floor_start, duration)}.
	 */
	private void collectDuration(Pair<Long, Long> session) {
		List<Long> durations = collectedDurations.get(session.getKey());

		if (durations == null) {
			durations = new ArrayList<>();
			collectedDurations.put(session.getKey(), durations);
		}

		durations.add(session.getValue());
	}

	/**
	 * Calculates the intensity based on the durations of the timestamp.
	 *
	 * @param timestamp
	 * @param durations
	 * @return {@code Pair(floor_start, intensity)}.
	 */
	private IntensityRecord calculateIntensity(long timestamp, List<Long> durations) {
		double sum = durations.stream().mapToLong(i -> i).sum();
		long intensity = Math.round(sum / resolutionMicros);

		IntensityRecord record = new IntensityRecord();
		record.setTimestamp(timestamp / 1000); // timestamp is in millis
		record.setIntensity(Collections.singletonMap(group, intensity));
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
