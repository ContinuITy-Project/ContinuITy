package org.continuity.idpa;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import org.continuity.idpa.application.Application;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Represents a {@link Version} or a {@link Date} timestamp.
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(using = VersionOrTimestamp.Serializer.class)
@JsonDeserialize(using = VersionOrTimestamp.Deserializer.class)
public class VersionOrTimestamp implements Comparable<VersionOrTimestamp> {

	public static final VersionOrTimestamp MAX_VALUE = new VersionOrTimestamp(new Version(Integer.toString(Integer.MAX_VALUE), Arrays.asList(Integer.MAX_VALUE)));

	public static final VersionOrTimestamp MIN_VALUE = new VersionOrTimestamp(new Date(0));

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(Application.DATE_FORMAT);

	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final Date timestamp;

	private final Version version;

	public VersionOrTimestamp(Version version) {
		this(version, null);
	}

	public VersionOrTimestamp(Date timestamp) {
		this(null, timestamp);
	}

	public VersionOrTimestamp(Version version, Date timestamp) {
		this.timestamp = timestamp;
		this.version = version;
	}

	/**
	 * Parses an instance from a string representation and automatically determines the type.
	 *
	 * @param string
	 *            The string representation.
	 * @return A {@link VersionOrTimestamp}.
	 * @throws ParseException
	 */
	public static VersionOrTimestamp fromString(String string) throws NumberFormatException, ParseException {
		if (string.matches(Version.REGEX)) {
			return new VersionOrTimestamp(Version.fromString(string));
		} else {
			return new VersionOrTimestamp(DATE_FORMAT.parse(string));
		}
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public Version getVersion() {
		return version;
	}

	public boolean isVersion() {
		return (version != null) || (timestamp == null);
	}

	public boolean isTimestamp() {
		return !isVersion();
	}

	public boolean isEmpty() {
		return (version == null) && (timestamp == null);
	}

	@Override
	public String toString() {
		if (isVersion()) {
			return Objects.toString(version, "[null/empty]");
		} else {
			return DATE_FORMAT.format(timestamp);
		}
	}

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 *
	 * <i>Version will always be treated newer than timestamps.</i>
	 */
	@Override
	public int compareTo(VersionOrTimestamp other) {
		if (other == null) {
			return 1;
		}

		if (this.isVersion() && other.isVersion()) {
			if (this.getVersion() == null) {
				return other.getVersion() == null ? 0 : -1;
			} else {
				return this.getVersion().compareTo(other.getVersion());
			}
		} else if (this.isTimestamp() && other.isTimestamp()) {
			return this.getTimestamp().compareTo(other.getTimestamp());
		} else if (this.isVersion()) {
			return 1;
		} else {
			return -1;
		}
	}

	public boolean before(VersionOrTimestamp other) {
		return compareTo(other) < 0;
	}

	public boolean after(VersionOrTimestamp other) {
		return compareTo(other) > 0;
	}

	@Override
	public int hashCode() {
		if (isVersion()) {
			return Objects.hash(getVersion());
		} else {
			return timestamp.hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || !(obj instanceof VersionOrTimestamp)) {
			return false;
		}

		return compareTo((VersionOrTimestamp) obj) == 0;
	}

	public static class Serializer extends StdSerializer<VersionOrTimestamp> {

		private static final long serialVersionUID = -273473244168915329L;

		protected Serializer() {
			super(VersionOrTimestamp.class);
		}

		@Override
		public void serialize(VersionOrTimestamp value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeString(value.toString());
		}

	}

	public static class Deserializer extends StdDeserializer<VersionOrTimestamp> {

		private static final long serialVersionUID = -951856046418174517L;

		protected Deserializer() {
			super(Version.class);
		}

		@Override
		public VersionOrTimestamp deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			String value = p.getValueAsString();

			try {
				return fromString(value);
			} catch (NumberFormatException | ParseException e) {
				throw new InvalidFormatException(p, e.getMessage(), value, VersionOrTimestamp.class);
			}
		}

	}

}
