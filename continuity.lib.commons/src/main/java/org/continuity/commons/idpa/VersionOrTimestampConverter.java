package org.continuity.commons.idpa;

import java.text.ParseException;

import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

/**
 * Will be used to convert strings to {@link VersionOrTimestamp}s in REST endpoints.
 * 
 * @author Henning Schulz
 *
 */
public class VersionOrTimestampConverter implements Converter<String, VersionOrTimestamp> {

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionOrTimestampConverter.class);

	@Override
	public VersionOrTimestamp convert(String source) {
		try {
			return VersionOrTimestamp.fromString(source);
		} catch (NumberFormatException | ParseException e) {
			LOGGER.error("Cannot parse version!", e);
			return null;
		}
	}

}
