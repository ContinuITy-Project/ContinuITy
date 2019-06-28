package org.continuity.idpa;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Represents an application and service ID: {@code app.service}.
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(converter = AppId.ToStringConverter.class)
@JsonDeserialize(converter = AppId.Converter.class)
public class AppId implements Comparable<AppId> {

	public static final String SERVICE_ALL = "all";

	public static final String DELIM = ".";

	private static final String DELIM_REGEX = "\\.";

	private final String application;

	private final String service;

	private AppId(String application, String service) {
		this.application = application;
		this.service = service;
	}

	public static AppId fromString(String id) {
		if (id.contains(DELIM)) {
			String[] parts = id.split(DELIM_REGEX);
			return new AppId(parts[0], parts[1]);
		} else {
			return new AppId(id, SERVICE_ALL);
		}
	}

	public String getApplication() {
		return application;
	}

	public String getService() {
		return service;
	}

	@Override
	public String toString() {
		if (SERVICE_ALL.equals(service)) {
			return application;
		} else {
			return new StringBuilder().append(application).append(DELIM).append(service).toString();
		}
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || !(obj instanceof AppId)) {
			return false;
		}

		AppId other = (AppId) obj;

		return Objects.equals(this.application, other.application) && Objects.equals(this.service, other.service);
	}

	@Override
	public int compareTo(AppId other) {
		return this.toString().compareTo(other.toString());
	}

	public static class Converter extends StdConverter<String, AppId> {

		@Override
		public AppId convert(String value) {
			return fromString(value);
		}

	}

	public static class ToStringConverter extends StdConverter<AppId, String> {

		@Override
		public String convert(AppId value) {
			return value.toString();
		}

	}

}
