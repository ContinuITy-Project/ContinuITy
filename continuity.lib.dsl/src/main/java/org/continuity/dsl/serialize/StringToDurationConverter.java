package org.continuity.dsl.serialize;

import java.time.Duration;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 *
 * @author Henning Schulz
 *
 */
public class StringToDurationConverter extends StdConverter<String, Duration> {

	@Override
	public Duration convert(String value) {
		return Duration.parse(value);
	}

}
