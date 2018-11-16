package org.continuity.dsl.description;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Future occurrences of a value.
 * @author Alper Hidiroglu
 */
public class FutureOccurrences {

	private List<Date> singleDates;
	private List<Pair<Date, Date>> rangeDates;
	
	@JsonIgnore
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	public FutureOccurrences(List<String> futureDates) {
		singleDates = new ArrayList<Date>();
		rangeDates = new ArrayList<Pair<Date, Date>>();
		String delims = "to";
		
		for(String date: futureDates) {
			String[] tokens = date.split(delims);
			if(tokens.length == 1) {
				Date d = null;
				try {
				    d = dateFormat.parse(tokens[0]);
				    // 13 digits
				} catch (ParseException e) {
				    e.printStackTrace();
				}
				singleDates.add(d);
			} else if (tokens.length == 2) {
			    Date dFrom = null;
			    Date dTo = null;
				try {
				    dFrom = dateFormat.parse(tokens[0]);
				    dTo = dateFormat.parse(tokens[1]);
				} catch (ParseException e) {
				    e.printStackTrace();
				}
				Pair<Date, Date> rangeTimestamp = new Pair<>(dFrom, dTo);
				rangeDates.add(rangeTimestamp);
			} else {
				System.out.println("Invalid context input!");
			}
		}
	}
	
	public FutureOccurrences() {
		
	}
	
	public List<Date> getSingleDates() {
		return singleDates;
	}

	public void setSingleDates(List<Date> singleDates) {
		this.singleDates = singleDates;
	}

	public List<Pair<Date, Date>> getRangeDates() {
		return rangeDates;
	}

	public void setRangeDates(List<Pair<Date, Date>> rangeTimestamps) {
		this.rangeDates = rangeTimestamps;
	}
	
	@JsonIgnore
	public List<Long> getFutureDatesAsTimestamps(long interval) {
		List<Long> timestamps = new ArrayList<Long>();
		for(Date date: this.singleDates) {
			long timestamp = date.getTime();
			timestamps.add(timestamp);
		}
		for(Pair<Date, Date> range: this.rangeDates) {
			long timestampFrom = range.getKey().getTime();
			long timestampTo = range.getValue().getTime();
			while(timestampFrom <= timestampTo) {
				timestamps.add(timestampFrom);
				timestampFrom += interval;
			}
		}		
		return timestamps;
	}

	@Override
	public String toString() {
		return "FutureOccurrences [single-dates=" + singleDates + ", range-dates=" + rangeDates + "]";
	}
}
