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
import org.continuity.commons.utils.IntensityCalculationUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.dsl.description.ForecastInput;
import org.continuity.dsl.description.IntensityCalculationInterval;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Manager for calculating intensities and saving them into database.
 * 
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
	public void setupDatabase() {
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
	 * 
	 * @param sessions
	 * @return
	 */
	private void calculateIntensitiesForUserGroup(List<SimplifiedSession> sessions, int behaviorId) {
		IntensityCalculationUtils.sortSessions(sessions);
		long startTime = sessions.get(0).getStartTime();

		if(null == forecastInput.getForecastOptions().getInterval()) {
			forecastInput.getForecastOptions().setInterval(IntensityCalculationInterval.SECOND);
		}
		
		// The time range for which an intensity will be calculated
		long rangeLength = forecastInput.getForecastOptions().getInterval().asNumber();

		// rounds start time down
		long roundedStartTime = startTime - startTime % rangeLength;

		long highestEndTime = 0;

		for (SimplifiedSession session : sessions) {
			if (session.getEndTime() > highestEndTime) {
				highestEndTime = session.getEndTime();
			}
		}

		// Check if overall session logs duration is shorter than a single range
		if (rangeLength > (highestEndTime - startTime)) {
			new RuntimeException("The intensity of the given session logs cannot be calculated, because the used range of " + rangeLength
					+ " nanos is longer than the overall duration of the session logs.");
		}

		// rounds highest end time up
		long roundedHighestEndTime = highestEndTime;
		if (highestEndTime % rangeLength != 0) {
			roundedHighestEndTime = (highestEndTime - highestEndTime % rangeLength) + rangeLength;
		}

		long completePeriod = roundedHighestEndTime - roundedStartTime;
		long amountOfRanges = completePeriod / rangeLength;

		ArrayList<Range<Long>> listOfRanges = IntensityCalculationUtils.calculateRanges(roundedStartTime, amountOfRanges, rangeLength);

		// Remove first and last range from list if necessary
		if (listOfRanges.get(0).getMinimum() != startTime) {
			listOfRanges.remove(0);
		}

		if (listOfRanges.get(listOfRanges.size() - 1).getMaximum() != highestEndTime) {
			listOfRanges.remove(listOfRanges.size() - 1);
		}

		// This map is used to hold necessary information which will be saved into DB
		HashMap<Long, Integer> intensities = new HashMap<Long, Integer>();

		for (Range<Long> range : listOfRanges) {
			ArrayList<SimplifiedSession> sessionsInRange = new ArrayList<SimplifiedSession>();
			for (SimplifiedSession session : sessions) {
				Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
				if (sessionRange.containsRange(range) || range.contains(session.getStartTime()) || range.contains(session.getEndTime())) {
					sessionsInRange.add(session);
				}
			}
			int intensityOfRange = (int) IntensityCalculationUtils.calculateIntensityForRange(range, sessionsInRange, rangeLength);
			intensities.put(range.getMinimum(), intensityOfRange);
		}

		saveIntensitiesOfUserGroupIntoDb(intensities, behaviorId);
	}

	/**
	 * Saves intensities into InfluxDB
	 * 
	 * @param intensities
	 */
	@SuppressWarnings("rawtypes")
	private void saveIntensitiesOfUserGroupIntoDb(HashMap<Long, Integer> intensities, int behaviorId) {
		String measurementName = "userGroup" + behaviorId;
		Iterator iterator = intensities.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry pair = (Map.Entry) iterator.next();
			Point point = Point.measurement(measurementName).time((long) pair.getKey(), TimeUnit.NANOSECONDS).addField("value", (int) pair.getValue()).build();

			influxDb.write(point);
			iterator.remove();
		}
	}
}
