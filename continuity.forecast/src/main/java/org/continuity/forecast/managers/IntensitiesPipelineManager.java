package org.continuity.forecast.managers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.util.Pair;
import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.commons.utils.WebUtils;
import org.continuity.dsl.description.ForecastInput;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


/**
 * Manager for calculating intensities and saving them into database.
 * @author Alper Hidiroglu
 *
 */
public class IntensitiesPipelineManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForecastPipelineManager.class);

	private RestTemplate restTemplate;
	
	private InfluxDB influxDb;
	
	private String tag;
	
	private ForecastInput forecastInput;
	
	private Pair<Date, Integer> dateAndAmountOfUserGroups;

	public Pair<Date, Integer> getDateAndAmountOfUserGroups() {
		return dateAndAmountOfUserGroups;
	}

	public void setDateAndAmountOfUserGroups(Pair<Date, Integer> dateAndAmountOfUsers) {
		this.dateAndAmountOfUserGroups = dateAndAmountOfUsers;
	}


	private int workloadIntensity;

	public int getWorkloadIntensity() {
		return workloadIntensity;
	}

	public void setWorkloadIntensity(int workloadIntensity) {
		this.workloadIntensity = workloadIntensity;
	}

	/**
	 * Constructor.
	 */
	public IntensitiesPipelineManager(RestTemplate restTemplate, InfluxDB influxDb, String tag, ForecastInput context) {
		this.restTemplate = restTemplate;
		this.influxDb = influxDb;
		this.tag = tag;
		this.forecastInput = context;
	}
	
	public IntensitiesPipelineManager() {
		
	}
	
	@SuppressWarnings("deprecation")
	public void setupDatabase(){
		String dbName = this.tag;
		if (!influxDb.describeDatabases().contains(dbName)) {
			influxDb.createDatabase(dbName);
		}
		influxDb.setDatabase(dbName);
		influxDb.setRetentionPolicy("autogen");
	}

	/**
	 * Runs the pipeline.
	 */
	public void runPipeline(String linkToSessions) {
		setupDatabase();
		SessionsBundlePack sessionsBundles = null;
		try {
			sessionsBundles = restTemplate.getForObject(WebUtils.addProtocolIfMissing(linkToSessions), SessionsBundlePack.class);
		} catch (RestClientException e) {
			LOGGER.error("Error when retrieving sessions!", e);
		}
		
		Date date = sessionsBundles.getTimestamp();
		int amountOfUsers = sessionsBundles.getSessionsBundles().size();
		Pair<Date, Integer> pairDateUserGroupAmount = new Pair<>(date, amountOfUsers);
		setDateAndAmountOfUserGroups(pairDateUserGroupAmount);
		
		influxDb.enableBatch(BatchOptions.DEFAULTS);
		
		calculateIntensities(sessionsBundles.getSessionsBundles());

		influxDb.disableBatch();
	}
		
	/**
	 * @param bundleList
	 */
	private void calculateIntensities(List<SessionsBundle> bundleList) {
		for (SessionsBundle sessBundle : bundleList) {
			List<SimplifiedSession> sessions = sessBundle.getSessions();
			int behaviorId = sessBundle.getBehaviorId();
			calculateIntensitiesForUserGroup(sessions, behaviorId);
		}
	}

	/**
	 * Calculates the intensities for one user group. Saves the intensities into database.
	 * Timestamps are in nanoseconds.
	 * @param sessions
	 * @return
	 */
	private void calculateIntensitiesForUserGroup(List<SimplifiedSession> sessions, int behaviorId) {
		sortSessions(sessions);
		long startTime = sessions.get(0).getStartTime();
		
		// The time range for which an intensity will be calculated
		long rangeLength = calculateInterval(forecastInput.getForecastOptions().getInterval());
		
		// rounds start time down
		long roundedStartTime = startTime - startTime % rangeLength;
		
		long highestEndTime = 0;
		
		for(SimplifiedSession session: sessions) {
			if(session.getEndTime() > highestEndTime) {
				highestEndTime = session.getEndTime();
			}
		}
		// rounds highest end time up
		long roundedHighestEndTime = highestEndTime;
		if (highestEndTime % rangeLength != 0) {
			roundedHighestEndTime = (highestEndTime - highestEndTime % rangeLength) + rangeLength;
		}
		
		long completePeriod = roundedHighestEndTime - roundedStartTime;
		long amountOfRanges = completePeriod / rangeLength;
		
		ArrayList<Range<Long>> listOfRanges = calculateRanges(roundedStartTime, amountOfRanges, rangeLength);
		
		// Remove first and last range from list if necessary
		if(listOfRanges.get(0).getMinimum() != startTime) {
			listOfRanges.remove(0);
		}
		
		if(listOfRanges.get(listOfRanges.size() - 1).getMaximum() != highestEndTime) {
			listOfRanges.remove(listOfRanges.size() - 1);
		}
		
		// This map is used to hold necessary information which will be saved into DB
		HashMap<Long, Integer> intensities = new HashMap<Long, Integer>();
		
		for(Range<Long> range: listOfRanges) {
			ArrayList<SimplifiedSession> sessionsInRange = new ArrayList<SimplifiedSession>();
			for(SimplifiedSession session: sessions) {
				Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
				if(sessionRange.containsRange(range) || range.contains(session.getStartTime()) 
						|| range.contains(session.getEndTime())) {
					sessionsInRange.add(session);
				}
			}
			int intensityOfRange = (int) calculateIntensityForRange(range, sessionsInRange, rangeLength);		
			intensities.put(range.getMinimum(), intensityOfRange);
		}
		
		saveIntensitiesOfUserGroupIntoDb(intensities, behaviorId);	
	}

	/**
	 * Saves intensities into InfluxDB
	 * @param intensities
	 */
	@SuppressWarnings("rawtypes")
	private void saveIntensitiesOfUserGroupIntoDb(HashMap<Long, Integer> intensities, int behaviorId) {
		String measurementName = "userGroup" + behaviorId;
		Iterator iterator = intensities.entrySet().iterator();
	    while (iterator.hasNext()) {
			Map.Entry pair = (Map.Entry)iterator.next();
			Point point = Point.measurement(measurementName)
					.time((long) pair.getKey(), TimeUnit.NANOSECONDS)
				    .addField("value", (int) pair.getValue()) 
					.build();
		
			influxDb.write(point);
	        iterator.remove(); 
	    }
	}

	protected long calculateInterval(String interval) {
		long numericInterval = 0;
		switch(interval) {
		   case "secondly":
		      numericInterval = 1000000000L;
		      break;		      
		   case "minutely":
			  numericInterval = 60000000000L;
			  break;		   
		   case "hourly":
		      numericInterval = 3600000000000L;
		      break;
		   default: 
			  numericInterval = 1000000000L;
		}
		return numericInterval;
	}

	/**
	 * Calculates the time ranges.
	 * @param startTime
	 * @param amountOfRanges
	 * @param rangeLength
	 * @return
	 */
	private ArrayList<Range<Long>> calculateRanges(long startTime, long amountOfRanges, long rangeLength ) {
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
	private long calculateIntensityForRange(Range<Long> range, ArrayList<SimplifiedSession> sessionsInRange, long rangeLength) {
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
	private void sortSessions(List<SimplifiedSession> sessions) {
		sessions.sort((SimplifiedSession sess1, SimplifiedSession sess2) -> {
			   if (sess1.getStartTime() > sess2.getStartTime())
			     return 1;
			   if (sess1.getStartTime() < sess2.getStartTime())
			     return -1;
			   return 0;
			});
	}	
}
