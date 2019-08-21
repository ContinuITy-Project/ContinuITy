package org.continuity.dsl.serialize;

import java.util.Optional;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 *
 * @author Henning Schulz
 *
 */
public class ContentToOptionalConverter<T> extends StdConverter<T, Optional<T>> {

	@Override
	public Optional<T> convert(T value) {
		return Optional.ofNullable(value);
	}

}
