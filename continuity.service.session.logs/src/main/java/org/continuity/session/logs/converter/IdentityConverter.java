package org.continuity.session.logs.converter;

import java.util.List;

import org.spec.research.open.xtrace.api.core.Trace;

/**
 * Converter that simply returns the passed traces.
 *
 * @author Henning Schulz
 *
 */
public class IdentityConverter implements OpenXtraceConverter<Trace> {

	@Override
	public List<Trace> convert(List<Trace> data) {
		return data;
	}

}
