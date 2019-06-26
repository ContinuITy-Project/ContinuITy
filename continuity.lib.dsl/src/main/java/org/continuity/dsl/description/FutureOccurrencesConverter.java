package org.continuity.dsl.description;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

public class FutureOccurrencesConverter implements Converter<FutureOccurrences, List<String>> {
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	@Override
	public List<String> convert(FutureOccurrences futOcc) {
		List<String> dates = new ArrayList<String>();
		for(Date date: futOcc.getSingleDates()) {
			dates.add(dateFormat.format(date));
		}
		for(Pair<Date, Date> datePair: futOcc.getRangeDates()) {
			String rangeDates = dateFormat.format(datePair.getKey()) + " " + "to" + " " + dateFormat.format(datePair.getValue());
			dates.add(rangeDates);
		}
		return dates;
	}

	@Override
	public JavaType getInputType(TypeFactory typeFactory) {
		return typeFactory.constructType(FutureOccurrences.class);
	}

	@Override
	public JavaType getOutputType(TypeFactory typeFactory) {
		return typeFactory.constructType(List.class);
	}
}
