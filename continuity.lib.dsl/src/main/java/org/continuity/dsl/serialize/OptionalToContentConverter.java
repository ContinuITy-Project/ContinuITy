package org.continuity.dsl.serialize;

import java.util.Optional;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Converts {@link Optional} to its content.
 *
 * @author Henning Schulz
 *
 */
public class OptionalToContentConverter<T> extends StdConverter<Optional<T>, T> {

	@Override
	public T convert(Optional<T> value) {
		return value.orElse(null);
	}

}
