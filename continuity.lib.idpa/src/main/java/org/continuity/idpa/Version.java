package org.continuity.idpa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Represents a version in the following format: <code>v?[0-9]+(.[0-9]+)*</code> <br>
 * <br>
 * Examples:
 * <ul>
 * <li><code>v2.1.45</code></li>
 * <li><code>2.1.45</code></li>
 * <li><code>v1</code></li>
 * </ul>
 *
 * <i>The following examples are <b>not</b> valid versions:</i>
 * <ul>
 * <li><code>v2.1.abc</code></li>
 * <li><code>X2.1.45</code></li>
 * <li><code>2-1-45</code></li>
 * </ul>
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(using = Version.Serializer.class)
@JsonDeserialize(using = Version.Deserializer.class)
public class Version implements Comparable<Version> {

	public static final String REGEX = "v?[0-9]+(?:\\.[0-9]+)*";

	private final String stringRepresentation;

	private final List<Integer> segments;

	public Version(String stringRepresentation, List<Integer> segments) {
		this.stringRepresentation = stringRepresentation;
		this.segments = segments;
	}

	/**
	 * Parses a version from the string representation.
	 *
	 * @param string
	 *            The string representation.
	 * @return The parsed version.
	 * @throws NumberFormatException
	 *             If at least one of the version segments could not be parsed as integer (except
	 *             for the first one, which can start with a v).
	 */
	public static Version fromString(String string) throws NumberFormatException {
		String orig = string;

		if (string.startsWith("v")) {
			string = string.substring(1);
		}

		List<Integer> segments = Arrays.stream(string.split("\\.")).map(Integer::parseInt).collect(Collectors.toList());

		return new Version(orig, segments);
	}

	public List<Integer> getSegments() {
		return segments;
	}

	/**
	 * Increases this version by a defined amount at a defined segment. E.g., for segment 2 and
	 * amount 3, the version {@code v1.2.0} will be increased to {@code v1.2.3}.
	 *
	 * @param segment
	 *            The segment number (0 = highest)
	 * @param amount
	 *            The number to be added to the segment.
	 * @return The increased version.
	 */
	public Version increase(int segment, int amount) {
		ArrayList<Integer> newSegments = new ArrayList<>();
		newSegments.addAll(segments);

		while (newSegments.size() < (segment + 1)) {
			newSegments.add(0);
		}

		newSegments.set(segment, newSegments.get(segment) + amount);

		String string = newSegments.stream().map(i -> Integer.toString(i)).collect(Collectors.joining("."));

		if (stringRepresentation.startsWith("v")) {
			string = "v" + string;
		}

		return new Version(string, newSegments);
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

	/**
	 * Returns a string that is guaranteed to be equal to the normalized string of equal versions.
	 * Can be different from the originally specified version.
	 *
	 * @return The string representation.
	 */
	public String toNormalizedString() {
		StringBuilder builder = new StringBuilder();
		builder.append("v");

		ListIterator<Integer> iterator = segments.listIterator(segments.size());

		while (iterator.hasPrevious() && (iterator.previous() == 0)) {
		}

		if (iterator.hasNext()) {
			iterator.next();
		}

		builder.append(segments.stream().limit(iterator.nextIndex()).map(Objects::toString).collect(Collectors.joining(".")));

		return builder.toString();
	}

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 *
	 * <i>The order is as follows:</i>
	 * <ul>
	 * <li><code>v1</code> (= <code>v1.0</code>)</li>
	 * <li><code>v1.1</code></li>
	 * <li><code>v2</code></li>
	 * <li><code>...</code></li>
	 * </ul>
	 *
	 */
	@Override
	public int compareTo(Version o) {
		Iterator<Integer> thiz = getSegments().iterator();
		Iterator<Integer> other = o.getSegments().iterator();

		while (thiz.hasNext() && other.hasNext()) {
			int thisSeg = thiz.next();
			int otherSeg = other.next();

			if (thisSeg != otherSeg) {
				return thisSeg - otherSeg;
			}
		}

		while (thiz.hasNext()) {
			if (thiz.next() > 0) {
				return 1;
			}
		}

		while (other.hasNext()) {
			if (other.next() > 0) {
				return -1;
			}
		}

		return 0;
	}

	@Override
	public int hashCode() {
		int hash = 0;

		for (int seg : segments) {
			hash = (31 * hash) + seg;
		}

		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || !(obj instanceof Version)) {
			return false;
		}

		return compareTo((Version) obj) == 0;
	}

	public static class Serializer extends StdSerializer<Version> {

		private static final long serialVersionUID = 5659736873161342904L;

		protected Serializer() {
			super(Version.class);
		}

		@Override
		public void serialize(Version value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(value.toString());
		}

	}

	public static class Deserializer extends StdDeserializer<Version> {

		private static final long serialVersionUID = 1625623488451340021L;

		protected Deserializer() {
			super(Version.class);
		}

		@Override
		public Version deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			return fromString(p.getValueAsString());
		}

	}

}
