package org.continuity.dsl.serialize;

import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * @author Henning Schulz
 *
 */
public class DurationToStringConverter extends StdConverter<Duration, String> {

	@Override
	public String convert(Duration value) {
		return Objects.toString(value);
	}

}
