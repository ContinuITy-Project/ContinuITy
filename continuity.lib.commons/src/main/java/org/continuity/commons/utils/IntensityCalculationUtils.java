package org.continuity.commons.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Range;
import org.continuity.api.entities.artifact.SimplifiedSession;

/**
 * This class provides utils which are needed in order to calculate the intensity of given session logs.
 * @author Alper Hidiroglu, Tobias Angerstein
 *
 */
public class IntensityCalculationUtils {
	/**
	 * Calculates the time ranges.
	 * @param startTime
	 * @param amountOfRanges
	 * @param rangeLength
	 * @return
	 */
	public static ArrayList<Range<Long>> calculateRanges(long startTime, long amountOfRanges, long rangeLength ) {
		ArrayList<Range<Long>> listOfRanges = new ArrayList<Range<Long>>();
		for(int i = 0; i < amountOfRanges; i++) {
			Range<Long> range = Range.between(startTime, startTime + rangeLength);
			listOfRanges.add(range);
			startTime += rangeLength;
		}
		return listOfRanges;
	}

	/**
	 * Calculates the workload intensity for a time range. Calculates average, min and max.
	 * @param range
	 * @param sessionsInRange
	 * @param rangeLength
	 * @return
	 */
	public static long calculateIntensityForRange(Range<Long> range, ArrayList<SimplifiedSession> sessionsInRange, long rangeLength) {
		int counter = 0;
		long sumOfTime = 0;
		boolean inTimeRange = true;
		long endOfRange = range.getMaximum();
		// smallest found timestamp
		long lastOccurredEvent = range.getMinimum();
		
		// initialize the counter with amount of sessions at the beginning of the range
		for(SimplifiedSession session: sessionsInRange) {
			Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
			if(sessionRange.contains(lastOccurredEvent)) {
				counter++;
			}
		}	
		// min value of range
		int minCounter = counter;
		// max value of range
		int maxCounter = counter;
		
		while(inTimeRange) {
			long minValue = Long.MAX_VALUE;
			int currentCounter = counter;
			for(SimplifiedSession session: sessionsInRange) {
				long startTimeOfSession = session.getStartTime();
				long endTimeOfSession = session.getEndTime();
				if(startTimeOfSession > lastOccurredEvent) {
					if(startTimeOfSession == minValue) {
						currentCounter ++;
					} else if (startTimeOfSession < minValue){
						currentCounter = counter + 1;
						minValue = startTimeOfSession;
					}
				} else if(endTimeOfSession > lastOccurredEvent) {
					if(endTimeOfSession == minValue) {
						currentCounter --;
					} else if (endTimeOfSession < minValue) {
						currentCounter = counter - 1;
						minValue = endTimeOfSession;
					}
				}
			} 
			if(minValue > endOfRange) {
				minValue = endOfRange;
				inTimeRange = false;
			}
			sumOfTime += counter * (minValue - lastOccurredEvent);
			lastOccurredEvent = minValue;
			
			counter = currentCounter;
			
			if(counter < minCounter) {
				minCounter = counter;
			} 
			if(counter > maxCounter) {
				maxCounter = counter;
			}		
		}
		return sumOfTime / rangeLength;
	}
	

	/**
	 * Sorts sessions.
	 * @param sessions
	 */
	public static void sortSessions(List<SimplifiedSession> sessions) {
		sessions.sort((SimplifiedSession sess1, SimplifiedSession sess2) -> {
			   if (sess1.getStartTime() > sess2.getStartTime())
			     return 1;
			   if (sess1.getStartTime() < sess2.getStartTime())
			     return -1;
			   return 0;
			});
	}

}
