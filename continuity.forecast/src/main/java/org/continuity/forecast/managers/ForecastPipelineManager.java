package org.continuity.forecast.managers;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.math3.util.Pair;
import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.dsl.description.ContextParameter;
import org.continuity.dsl.description.ForecastInput;
import org.continuity.dsl.description.FutureEvent;
import org.continuity.dsl.description.FutureEvents;
import org.continuity.dsl.description.FutureNumber;
import org.continuity.dsl.description.FutureNumbers;
import org.continuity.dsl.description.FutureOccurrences;
import org.continuity.dsl.description.IntensityCalculationInterval;
import org.continuity.dsl.description.Measurement;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.rosuda.JRI.Rengine;

/**
 * Manager for the workload forecasting.
 * 
 * @author Alper Hidiroglu
 *
 */
public class ForecastPipelineManager {

	private static final long NANOS_TO_MILLIS_FACTOR = 1000000;

	private InfluxDB influxDb;

	private String tag;

	private ForecastInput forecastInput;

	private int workloadIntensity;
	
	private IntensityCalculationInterval interval;


	public int getWorkloadIntensity() {
		return workloadIntensity;
	}

	public void setWorkloadIntensity(int workloadIntensity) {
		this.workloadIntensity = workloadIntensity;
	}

	/**
	 * Constructor.
	 */
	public ForecastPipelineManager(InfluxDB influxDb, String tag, ForecastInput forecastInput) {
		this.influxDb = influxDb;
		this.tag = tag;
		this.forecastInput = forecastInput;
		if(null == this.forecastInput.getForecastOptions().getInterval()) {
			interval = IntensityCalculationInterval.SECOND;
		} else {
			interval = this.forecastInput.getForecastOptions().getInterval();
		}
	}

	public void setupDatabase() {
		String dbName = this.tag;
		influxDb.setDatabase(dbName);
		influxDb.setRetentionPolicy("autogen");
	}

	/**
	 * Runs the pipeline.
	 *
	 * @return The generated forecast bundle.
	 */
	public ForecastBundle runPipeline(Pair<Date, Integer> dateAndAmountOfUsers) {
		setupDatabase();
		ForecastBundle forecastBundle = generateForecastBundle(dateAndAmountOfUsers);

		return forecastBundle;
	}

	/**
	 * Generates the forecast bundle.
	 * 
	 * @param logs
	 * @return
	 * @throws IOException
	 * @throws ExtractionException
	 * @throws ParseException
	 */
	private ForecastBundle generateForecastBundle(Pair<Date, Integer> dateAndAmountOfUsers) {
		// initialize intensity
		this.workloadIntensity = 1;
		// updates also the workload intensity
		LinkedList<Double> probabilities = forecastWorkload(dateAndAmountOfUsers.getValue());
		// forecast result
		return new ForecastBundle(dateAndAmountOfUsers.getKey(), this.workloadIntensity, probabilities);
	}

	/**
	 * Returns aggregated workload intensity and adapted behavior mix probabilities.
	 * 
	 * @param bundleList
	 * @return
	 */
	private LinkedList<Double> forecastWorkload(int amountOfUserGroups) {
		Rengine re = initializeRengine();

		LinkedList<Double> probabilities = new LinkedList<Double>();
		int sumOfIntensities = 0;
		List<Integer> forecastedIntensities = new LinkedList<Integer>();

		if (forecastInput.getForecastOptions().getForecaster().equalsIgnoreCase("Telescope")) {
			initializeTelescope(re);

			for (int i = 0; i < amountOfUserGroups; i++) {
				int intensity = forecastIntensityForUserGroupTelescope(i, re);
				forecastedIntensities.add(intensity);
				sumOfIntensities += intensity;
			}
		} else if (forecastInput.getForecastOptions().getForecaster().equalsIgnoreCase("Prophet")) {
			initializeProphet(re);

			for (int i = 0; i < amountOfUserGroups; i++) {
				int intensity = forecastIntensityForUserGroupProphet(i, re);
				forecastedIntensities.add(intensity);
				sumOfIntensities += intensity;
			}
		}
		re.end();
		// updates the workload intensity
		setWorkloadIntensity(sumOfIntensities);

		for (int intensity : forecastedIntensities) {
			double probability = (double) intensity / (double) sumOfIntensities;
			probabilities.add(probability);
		}
		return probabilities;
	}

	/**
	 * Forecasting the intensities for a user group using Prophet.
	 * 
	 * @param i
	 * @param re
	 * @return
	 */
	private int forecastIntensityForUserGroupProphet(int i, Rengine re) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getDefault());

		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = getIntensitiesOfUserGroupFromDatabase(i);
		ArrayList<Long> timestampsOfIntensities = timestampsAndIntensities.getKey();
		ArrayList<Double> intensities = timestampsAndIntensities.getValue();

		Pair<ArrayList<Pair<String, ArrayList<Double>>>, ArrayList<Pair<String, ArrayList<Double>>>> covariates = calculateCovariates(timestampsOfIntensities, intensities);

		ArrayList<String> datesOfIntensities = new ArrayList<String>();
		for (long timestamp : timestampsOfIntensities) {
			Date date = new Date();
			date.setTime((long) timestamp);
			String resultDateFromTimestamp = sdf.format(date);
			datesOfIntensities.add(resultDateFromTimestamp);
		}
		int size = calculateSizeOfForecast(timestampsOfIntensities);
		int intensity = forecastWithProphet(datesOfIntensities, intensities, covariates.getKey(), covariates.getValue(), size, re);
		return intensity;
	}

	/**
	 * Counts amount of forecasted values subway.
	 * 
	 * @param timestampsOfIntensities
	 *            timestamps
	 * @return the amount
	 */
	private int calculateSizeOfForecast(ArrayList<Long> timestampsOfIntensities) {
		long endTimeIntensities = timestampsOfIntensities.get(timestampsOfIntensities.size() - 1);
		long endTimeForecast = this.forecastInput.getForecastOptions().getDateAsTimestamp();
		long interval = this.interval.asNumber() / NANOS_TO_MILLIS_FACTOR;

		// Calculates considered future timestamps
		long startTimeForecast = endTimeIntensities + interval;
		int size = 0;
		while (startTimeForecast <= endTimeForecast) {
			size++;
			startTimeForecast += interval;
		}
		return size;
	}

	/**
	 * Forecasting the intensities for a user group using Telescope.
	 * 
	 * @param i
	 * @param re
	 * @return
	 */
	private int forecastIntensityForUserGroupTelescope(int i, Rengine re) {
		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = getIntensitiesOfUserGroupFromDatabase(i);
		ArrayList<Long> timestampsOfIntensities = timestampsAndIntensities.getKey();
		ArrayList<Double> intensities = timestampsAndIntensities.getValue();

		Pair<ArrayList<Pair<String, ArrayList<Double>>>, ArrayList<Pair<String, ArrayList<Double>>>> covariates = calculateCovariates(timestampsOfIntensities, intensities);
		int size = calculateSizeOfForecast(timestampsOfIntensities);
		int intensity = forecastWithTelescope(intensities, covariates.getKey(), covariates.getValue(), size, re);
		return intensity;
	}

	/**
	 * Calculates historical and future covariates.
	 * 
	 * @param timestampsOfIntensities
	 * @param intensities
	 * @return
	 */
	private Pair<ArrayList<Pair<String, ArrayList<Double>>>, ArrayList<Pair<String, ArrayList<Double>>>> calculateCovariates(ArrayList<Long> timestampsOfIntensities, ArrayList<Double> intensities) {

		ArrayList<Pair<String, ArrayList<Double>>> histCovariates = new ArrayList<Pair<String, ArrayList<Double>>>();
		ArrayList<Pair<String, ArrayList<Double>>> futureCovariates = new ArrayList<Pair<String, ArrayList<Double>>>();

		if (forecastInput.getContext() != null) {

			long startTimeOfIntensities = timestampsOfIntensities.get(0);
			long endTimeIntensities = timestampsOfIntensities.get(timestampsOfIntensities.size() - 1);

			long endTimeForecast = this.forecastInput.getForecastOptions().getDateAsTimestamp();
			
			long interval = this.interval.asNumber() /NANOS_TO_MILLIS_FACTOR;

			// Calculates considered future timestamps
			long startTimeForecast = endTimeIntensities + interval;
			long forecastTimestamp = startTimeForecast;
			ArrayList<Long> futureTimestamps = new ArrayList<Long>();
			while (forecastTimestamp <= endTimeForecast) {
				futureTimestamps.add(forecastTimestamp);
				forecastTimestamp += interval;
			}

			for (ContextParameter covar : forecastInput.getContext()) {
				if (covar instanceof FutureNumbers) {
					FutureNumbers numCovar = (FutureNumbers) covar;

					ArrayList<Double> historicalOccurrences = calculateHistoricalOccurrencesNumerical(numCovar, timestampsOfIntensities, startTimeOfIntensities, endTimeIntensities);
					ArrayList<Double> futureOccurrences = calculateFutureOccurrencesNumerical(numCovar, futureTimestamps, startTimeForecast, endTimeForecast);

					List<FutureNumber> numericInstances = numCovar.getFuture();

					for (FutureNumber numInstance : numericInstances) {
						double value = numInstance.getValue();
						FutureOccurrences futureTimes = numInstance.getTime();
						List<Long> futureTimestampsOfValue = futureTimes.getFutureDatesAsTimestamps(interval);

						for (long futureTimestampOfValue : futureTimestampsOfValue) {
							int index = futureTimestamps.indexOf(futureTimestampOfValue);
							futureOccurrences.set(index, value);
						}
					}
					Pair<String, ArrayList<Double>> covarPast = new Pair<>(numCovar.getMeasurement(), historicalOccurrences);
					Pair<String, ArrayList<Double>> covarFuture = new Pair<>(numCovar.getMeasurement(), futureOccurrences);
					histCovariates.add(covarPast);
					futureCovariates.add(covarFuture);

				} else if (covar instanceof FutureEvents) {
					FutureEvents stringCovar = (FutureEvents) covar;
					List<FutureEvent> stringInstances = stringCovar.getFuture();

					HashMap<String, ArrayList<Long>> eventMap = calculateEventMap(stringCovar, startTimeOfIntensities, endTimeIntensities, startTimeForecast, endTimeForecast);

					for (Map.Entry<String, ArrayList<Long>> entry : eventMap.entrySet()) {
						String value = entry.getKey();
						ArrayList<Long> timestamps = entry.getValue();

						ArrayList<Double> historicalOccurrences = new ArrayList<Double>(Collections.nCopies(timestampsOfIntensities.size(), 0.0));
						ArrayList<Double> futureOccurrences = new ArrayList<Double>(Collections.nCopies(futureTimestamps.size(), 0.0));

						calculateOccurrencesString(futureOccurrences, historicalOccurrences, timestamps, timestampsOfIntensities, futureTimestamps, endTimeIntensities);

						for (FutureEvent stringInstance : stringInstances) {
							String valueOfInstance = stringInstance.getValue();
							if (valueOfInstance.equals(value)) {
								FutureOccurrences futureTimes = stringInstance.getTime();
								List<Long> futureTimestampsOfValue = futureTimes.getFutureDatesAsTimestamps(interval);

								for (long futureTimestampOfValue : futureTimestampsOfValue) {
									int index = futureTimestamps.indexOf(futureTimestampOfValue);
									futureOccurrences.set(index, 1.0);
								}
							}
						}
						Pair<String, ArrayList<Double>> covarPast = new Pair<>(value, historicalOccurrences);
						Pair<String, ArrayList<Double>> covarFuture = new Pair<>(value, futureOccurrences);
						histCovariates.add(covarPast);
						futureCovariates.add(covarFuture);
					}

				} else if (covar instanceof Measurement) {
					Measurement mCovar = (Measurement) covar;
					boolean isNumeric = identifyIfNumericValues(mCovar);
					if (isNumeric) {

						ArrayList<Double> historicalOccurrences = calculateHistoricalOccurrencesNumerical(mCovar, timestampsOfIntensities, startTimeOfIntensities, endTimeIntensities);
						ArrayList<Double> futureOccurrences = calculateFutureOccurrencesNumerical(mCovar, futureTimestamps, startTimeForecast, endTimeForecast);

						Pair<String, ArrayList<Double>> covarPast = new Pair<>(mCovar.getMeasurement(), historicalOccurrences);
						Pair<String, ArrayList<Double>> covarFuture = new Pair<>(mCovar.getMeasurement(), futureOccurrences);
						histCovariates.add(covarPast);
						futureCovariates.add(covarFuture);

					} else {

						HashMap<String, ArrayList<Long>> eventMap = calculateEventMap(mCovar, startTimeOfIntensities, endTimeIntensities, startTimeForecast, endTimeForecast);

						for (Map.Entry<String, ArrayList<Long>> entry : eventMap.entrySet()) {
							ArrayList<Long> timestamps = entry.getValue();

							ArrayList<Double> historicalOccurrences = new ArrayList<Double>(Collections.nCopies(timestampsOfIntensities.size(), 0.0));
							ArrayList<Double> futureOccurrences = new ArrayList<Double>(Collections.nCopies(futureTimestamps.size(), 0.0));

							calculateOccurrencesString(futureOccurrences, historicalOccurrences, timestamps, timestampsOfIntensities, futureTimestamps, endTimeIntensities);

							Pair<String, ArrayList<Double>> covarPast = new Pair<>(entry.getKey(), historicalOccurrences);
							Pair<String, ArrayList<Double>> covarFuture = new Pair<>(entry.getKey(), futureOccurrences);
							histCovariates.add(covarPast);
							futureCovariates.add(covarFuture);
						}
					}
				}
			}
		}
		Pair<ArrayList<Pair<String, ArrayList<Double>>>, ArrayList<Pair<String, ArrayList<Double>>>> covariates = new Pair<>(histCovariates, futureCovariates);
		return covariates;
	}

	/**
	 * Calculates occurrences for Strings.
	 * 
	 * @param futureOccurrences
	 * @param historicalOccurrences
	 * @param timestamps
	 * @param timestampsOfIntensities
	 * @param futureTimestamps
	 * @param endTimeIntensities
	 */
	private void calculateOccurrencesString(ArrayList<Double> futureOccurrences, ArrayList<Double> historicalOccurrences, ArrayList<Long> timestamps, ArrayList<Long> timestampsOfIntensities,
			ArrayList<Long> futureTimestamps, long endTimeIntensities) {

		for (long timestamp : timestamps) {
			if (timestamp <= endTimeIntensities) {
				int index = timestampsOfIntensities.indexOf(timestamp);
				historicalOccurrences.set(index, 1.0);
			} else {
				int index = futureTimestamps.indexOf(timestamp);
				futureOccurrences.set(index, 1.0);
			}
		}
	}

	/**
	 * Calculates a HashMap containing all event (String) covariates.
	 * 
	 * @param mCovar
	 * @param startTime
	 * @param endTimeIntensities
	 * @param startTimeForecast
	 * @param endTimeForecast
	 * @return
	 */
	private HashMap<String, ArrayList<Long>> calculateEventMap(Measurement mCovar, long startTime, long endTimeIntensities, long startTimeForecast, long endTimeForecast) {
		HashMap<String, ArrayList<Long>> eventMap = new HashMap<String, ArrayList<Long>>();

		ArrayList<Pair<Long, String>> histValues = getStringValues(mCovar, convertTimestampToUtcDate(startTime), convertTimestampToUtcDate(endTimeIntensities));
		ArrayList<Pair<Long, String>> futValues = getStringValues(mCovar, convertTimestampToUtcDate(startTimeForecast), convertTimestampToUtcDate(endTimeForecast));

		for (Pair<Long, String> histValue : histValues) {
			String value = histValue.getValue();
			long timestamp = histValue.getKey();
			if (eventMap.containsKey(value)) {
				eventMap.get(value).add(timestamp);
			} else {
				ArrayList<Long> timestamps = new ArrayList<Long>();
				timestamps.add(timestamp);
				eventMap.put(value, timestamps);
			}
		}

		// Only future values where corresponding past values exist
		for (Pair<Long, String> futValue : futValues) {
			String value = futValue.getValue();
			if (eventMap.containsKey(value)) {
				eventMap.get(value).add(futValue.getKey());
			}
		}
		return eventMap;
	}

	/**
	 * Calculates historical occurrences for doubles.
	 * 
	 * @param mCovar
	 * @param timestampsOfIntensities
	 * @param startTime
	 * @param endTimeIntensities
	 * @return
	 */
	private ArrayList<Double> calculateHistoricalOccurrencesNumerical(Measurement mCovar, ArrayList<Long> timestampsOfIntensities, long startTime, long endTimeIntensities) {
		ArrayList<Double> historicalOccurrences = new ArrayList<Double>(Collections.nCopies(timestampsOfIntensities.size(), 0.0));
		ArrayList<Pair<Long, Double>> histValues = getNumericValues(mCovar, convertTimestampToUtcDate(startTime), convertTimestampToUtcDate(endTimeIntensities));

		for (Pair<Long, Double> histValue : histValues) {
			int index = timestampsOfIntensities.indexOf(histValue.getKey());
			historicalOccurrences.set(index, histValue.getValue());
		}

		return historicalOccurrences;
	}

	/**
	 * Calculates future occurrences for doubles.
	 * 
	 * @param mCovar
	 * @param futureTimestamps
	 * @param startTimeForecast
	 * @param endTimeForecast
	 * @return
	 */
	private ArrayList<Double> calculateFutureOccurrencesNumerical(Measurement mCovar, ArrayList<Long> futureTimestamps, long startTimeForecast, long endTimeForecast) {
		ArrayList<Double> futureOccurrences = new ArrayList<Double>(Collections.nCopies(futureTimestamps.size(), 0.0));
		ArrayList<Pair<Long, Double>> futValues = getNumericValues(mCovar, convertTimestampToUtcDate(startTimeForecast), convertTimestampToUtcDate(endTimeForecast));

		for (Pair<Long, Double> futValue : futValues) {
			int index = futureTimestamps.indexOf(futValue.getKey());
			futureOccurrences.set(index, futValue.getValue());
		}
		return futureOccurrences;
	}

	/**
	 * Passes intensities dataset and covariates to Prophet which does the forecasting. Aggregates
	 * the resulting intensities to one intensity value.
	 * 
	 * @param datesOfIntensities
	 * @param intensitiesOfUserGroup
	 * @param size
	 * @param re
	 * @return
	 */
	private int forecastWithProphet(ArrayList<String> datesOfIntensities, ArrayList<Double> intensitiesOfUserGroup, ArrayList<Pair<String, ArrayList<Double>>> covariates,
			ArrayList<Pair<String, ArrayList<Double>>> futureCovariates, int size, Rengine re) {

		double[] intensities = intensitiesOfUserGroup.stream().mapToDouble(i -> i).toArray();
		String[] dates = new String[datesOfIntensities.size()];
		dates = datesOfIntensities.toArray(dates);

		re.assign("dates", dates);
		re.assign("intensities", intensities);

		re.eval("source(\"prophet/InitializeVariables.R\")");

		if (!covariates.isEmpty()) {
			for (Pair<String, ArrayList<Double>> covariate : covariates) {
				double[] values = covariate.getValue().stream().mapToDouble(i -> i).toArray();
				re.assign("values", values);
				re.assign("covarname", covariate.getKey());
				re.eval("source(\"prophet/AddRegressors.R\")");
			}
		}

		String period = Integer.toString(size);
		re.assign("period", period);

		re.eval("source(\"prophet/FitModelAndCreateFutureDataframe.R\")");

		if (!covariates.isEmpty()) {
			int x = 0;
			for (Pair<String, ArrayList<Double>> covariate : covariates) {
				covariate.getValue().addAll(futureCovariates.get(x).getValue());
				double[] values = covariate.getValue().stream().mapToDouble(i -> i).toArray();
				re.assign("values", values);
				re.assign("covarname", covariate.getKey());
				re.eval("source(\"prophet/ExtendFutureDataframe.R\")");
				x++;
			}
		}

		re.eval("source(\"prophet/ForecastProphet.R\")");

		double[] forecastedIntensities = re.eval("forecastValues").asDoubleArray();

		return aggregateWorkload(forecastedIntensities);
	}

	/**
	 * Passes intensities dataset and covariates to Telescope which does the forecasting. Aggregates
	 * the resulting intensities to one intensity value.
	 * 
	 * @param intensitiesOfUserGroup
	 * @return
	 */
	private int forecastWithTelescope(ArrayList<Double> intensitiesOfUserGroup, ArrayList<Pair<String, ArrayList<Double>>> covariates, ArrayList<Pair<String, ArrayList<Double>>> futureCovariates,
			int size, Rengine re) {
		if (!covariates.isEmpty()) {
			// hist.covar
			String matrixString = calculateMatrix("hist", covariates, re);
			re.assign("hist.covar.matrix", re.eval(matrixString));

			// future.covar
			String futureMatrixString = calculateMatrix("future", futureCovariates, re);
			re.assign("future.covar.matrix", re.eval(futureMatrixString));
		}

		double[] intensities = intensitiesOfUserGroup.stream().mapToDouble(i -> i).toArray();

		String period = Integer.toString(size);

		re.assign("intensities", intensities);
		re.assign("period", period);

		if (!covariates.isEmpty()) {
			re.eval("source(\"telescope-multi/ForecastTelescopeWithCovariates.R\")");
		} else {
			re.eval("source(\"telescope-multi/ForecastTelescope.R\")");
		}
		re.eval("dev.off()");

		double[] forecastedIntensities = re.eval("forecastValues").asDoubleArray();

		return aggregateWorkload(forecastedIntensities);
	}

	/**
	 * Aggregates the forecasted workload. TODO: Check other possibilities for workload aggregation.
	 * Information should be passed by user.
	 * 
	 * @param forecastedIntensities
	 * @return
	 */
	private int aggregateWorkload(double[] forecastedIntensities) {
		double maxIntensity = 0;
		for (int i = 0; i < forecastedIntensities.length; i++) {
			if (forecastedIntensities[i] > maxIntensity) {
				maxIntensity = forecastedIntensities[i];
			}
		}
		return (int) Math.round(maxIntensity);
	}

	/**
	 * Calculates a covariate matrix. Such a matrix is an input to Telescope.
	 * 
	 * @param string
	 * @param covariates
	 * @param re
	 * @return
	 */
	private String calculateMatrix(String string, ArrayList<Pair<String, ArrayList<Double>>> covariates, Rengine re) {
		ArrayList<String> nameOfCovars = new ArrayList<String>();
		for (Pair<String, ArrayList<Double>> covariateValues : covariates) {
			String name = covariateValues.getKey() + "." + string;
			double[] occurrences = covariateValues.getValue().stream().mapToDouble(i -> i).toArray();
			re.assign(name, occurrences);
			nameOfCovars.add(name);
		}

		String matrixString = "cbind(";

		boolean isFirst = true;
		for (String name : nameOfCovars) {
			if (isFirst) {
				matrixString += name;
				isFirst = false;
			} else {
				matrixString += "," + name;
			}
		}
		matrixString += ")";
		return matrixString;
	}

	/**
	 * Converts a milliseconds timestamp to UTC date as string.
	 * 
	 * @param timestamp
	 * @return
	 */
	private String convertTimestampToUtcDate(long timestamp) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String date = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).format(dtf).toString();
		return date;
	}

	/**
	 * Tests if values in a measurement are numerical.
	 * 
	 * @param mCovar
	 * @return
	 */
	private boolean identifyIfNumericValues(Measurement mCovar) {
		String measurementName = mCovar.getMeasurement();
		String queryString = "SELECT time, value FROM " + measurementName;
		Query query = new Query(queryString, tag);
		QueryResult queryResult = influxDb.query(query);
		Result result = queryResult.getResults().get(0);
		if (result.getSeries() != null) {
			Series serie = result.getSeries().get(0);
			if (serie.getValues().get(0).get(1).getClass().toString().equalsIgnoreCase("class java.lang.Double")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets numeric values from database.
	 * 
	 * @param covariateValues
	 * @param covar
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private ArrayList<Pair<Long, Double>> getNumericValues(Measurement covar, String startTime, String endTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		ArrayList<Pair<Long, Double>> measurements = new ArrayList<Pair<Long, Double>>();
		String measurementName = covar.getMeasurement();
		String queryString = "SELECT time, value FROM " + measurementName + " WHERE time >= '" + startTime + "' AND time <= '" + endTime + "'";
		Query query = new Query(queryString, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			if (result.getSeries() != null) {
				for (Series serie : result.getSeries()) {
					for (List<Object> listTuples : serie.getValues()) {
						long time = 0;
						try {
							time = sdf.parse((String) listTuples.get(0)).getTime();
						} catch (ParseException e) {

						}
						double measurement = (double) listTuples.get(1);
						Pair<Long, Double> timeAndValue = new Pair<>(time, measurement);
						measurements.add(timeAndValue);
					}
				}
			}
		}
		return measurements;
	}

	/**
	 * Gets String values from database.
	 * 
	 * @param covar
	 * @param startTime
	 * @param endTime
	 */
	private ArrayList<Pair<Long, String>> getStringValues(Measurement covar, String startTime, String endTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		ArrayList<Pair<Long, String>> events = new ArrayList<Pair<Long, String>>();
		String measurementName = covar.getMeasurement();
		String queryString = "SELECT time, value FROM " + measurementName + " WHERE time >= '" + startTime + "' AND time <= '" + endTime + "'";
		Query query = new Query(queryString, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			if (result.getSeries() != null) {
				for (Series serie : result.getSeries()) {
					for (List<Object> listTuples : serie.getValues()) {
						long time = 0;
						try {
							time = sdf.parse((String) listTuples.get(0)).getTime();
						} catch (ParseException e) {

						}
						String event = (String) listTuples.get(1);
						Pair<Long, String> timeAndValue = new Pair<>(time, event);
						events.add(timeAndValue);
					}
				}
			}
		}
		return events;
	}

	/**
	 * Gets the intensities of a user group from database.
	 * 
	 * @param userGroupId
	 * @return
	 */
	public Pair<ArrayList<Long>, ArrayList<Double>> getIntensitiesOfUserGroupFromDatabase(int userGroupId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Pair<ArrayList<Long>, ArrayList<Double>> timestampsAndIntensities = null;
		ArrayList<Long> timestamps = new ArrayList<Long>();
		ArrayList<Double> intensities = new ArrayList<Double>();
		String measurementName = "userGroup" + userGroupId;
		Query query = new Query("SELECT time, value FROM " + measurementName, tag);
		QueryResult queryResult = influxDb.query(query);
		for (Result result : queryResult.getResults()) {
			if (result.getSeries() != null) {
				for (Series serie : result.getSeries()) {
					for (List<Object> listTuples : serie.getValues()) {
						long time = 0;
						try {
							time = sdf.parse((String) listTuples.get(0)).getTime();
						} catch (ParseException e) {

						}
						double intensity = (double) listTuples.get(1);
						timestamps.add(time);
						intensities.add(intensity);
					}
				}
			}
		}
		timestampsAndIntensities = new Pair<>(timestamps, intensities);
		return timestampsAndIntensities;
	}

	/**
	 * Initializes Telescope.
	 * 
	 * @param re
	 */
	private void initializeTelescope(Rengine re) {
		re.eval("source(\"telescope-multi/R/telescope.R\")");
		re.eval("source(\"telescope-multi/R/cluster_periods.R\")");
		re.eval("source(\"telescope-multi/R/detect_anoms.R\")");
		re.eval("source(\"telescope-multi/R/fitting_models.R\")");
		re.eval("source(\"telescope-multi/R/frequency.R\")");
		re.eval("source(\"telescope-multi/R/outlier.R\")");
		re.eval("source(\"telescope-multi/R/telescope_Utils.R\")");
		re.eval("source(\"telescope-multi/R/vec_anom_detection.R\")");
		re.eval("source(\"telescope-multi/R/xgb.R\")");

		re.eval("library(xgboost)");
		re.eval("library(cluster)");
		re.eval("library(forecast)");
		re.eval("library(e1071)");
	}

	/**
	 * Initializes Prophet.
	 * 
	 * @param re
	 */
	private void initializeProphet(Rengine re) {
		re.eval("library(prophet)");
	}

	/**
	 * Initializes Rengine.
	 * 
	 * @return
	 */
	private Rengine initializeRengine() {
		String newargs1[] = { "--no-save" };

		Rengine re = Rengine.getMainEngine();
		if (re == null) {
			re = new Rengine(newargs1, false, null);
		}
		re.eval(".libPaths('/usr/local/lib/R/site-library/')");
		return re;
	}
}
