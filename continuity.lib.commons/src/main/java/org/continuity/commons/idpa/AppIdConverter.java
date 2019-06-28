package org.continuity.commons.idpa;

import org.continuity.idpa.AppId;
import org.springframework.core.convert.converter.Converter;

/**
 * Will be used to convert strings to {@link AppId}s in REST endpoints.
 *
 * @author Henning Schulz
 *
 */
public class AppIdConverter implements Converter<String, AppId> {

	private final AppId.Converter converter = new AppId.Converter();

	@Override
	public AppId convert(String source) {
		return converter.convert(source);
	}

}
