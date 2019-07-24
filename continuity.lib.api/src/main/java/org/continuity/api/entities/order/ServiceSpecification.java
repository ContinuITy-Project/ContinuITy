package org.continuity.api.entities.order;

import java.text.ParseException;

import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonSerialize(converter = ServiceSpecification.SerConverter.class)
@JsonDeserialize(converter = ServiceSpecification.DeserConverter.class)
public class ServiceSpecification {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceSpecification.class);

	private final String service;

	private final VersionOrTimestamp version;

	public ServiceSpecification(String service, VersionOrTimestamp version) {
		this.service = service;
		this.version = version;
	}

	public static ServiceSpecification fromString(String value) {
		String[] parts = value.split("@");

		VersionOrTimestamp version = null;

		if (parts.length > 1) {
			try {
				version = VersionOrTimestamp.fromString(parts[1]);
			} catch (NumberFormatException | ParseException e) {
				LOGGER.error("Could not parse version or timestamp!", e);
			}
		}

		return new ServiceSpecification(parts[0], version);
	}

	public String getService() {
		return service;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public ServiceSpecification withVersionFallback(VersionOrTimestamp version) {
		if (((this.version == null) || this.version.isEmpty()) && ((version != null) && !version.isEmpty())) {
			return new ServiceSpecification(this.service, version);
		} else {
			return this;
		}
	}

	public static class SerConverter extends StdConverter<ServiceSpecification, String> {

		@Override
		public String convert(ServiceSpecification value) {
			StringBuilder builder = new StringBuilder().append(value.getService());

			if ((value.getVersion() != null) && !value.getVersion().isEmpty()) {
				builder.append("@").append(value.getVersion());
			}

			return builder.toString();
		}

	}

	public static class DeserConverter extends StdConverter<String, ServiceSpecification> {

		@Override
		public ServiceSpecification convert(String value) {
			return fromString(value);
		}

	}

}
