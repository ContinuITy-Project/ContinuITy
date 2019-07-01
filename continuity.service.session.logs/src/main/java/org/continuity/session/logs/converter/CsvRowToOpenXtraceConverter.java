package org.continuity.session.logs.converter;

import java.util.List;

import org.continuity.session.logs.entities.CsvRow;
import org.spec.research.open.xtrace.api.core.Trace;

/**
 * Converts {@link CsvRow} to {@link Trace}.
 *
 * @author Henning Schulz
 *
 */
public class CsvRowToOpenXtraceConverter implements OpenXtraceConverter<CsvRow> {

	@Override
	public List<Trace> convert(List<CsvRow> data) {
		// TODO Auto-generated method stub
		return null;
	}

}
