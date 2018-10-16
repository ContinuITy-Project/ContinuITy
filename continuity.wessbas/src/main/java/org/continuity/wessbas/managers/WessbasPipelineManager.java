package org.continuity.wessbas.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.Range;
import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.commons.utils.IntensityCalculationUtils;
import org.continuity.commons.utils.SimplifiedSessionLogsDeserializer;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.config.ModularizationApproach;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.commons.utils.WebUtils;
import org.continuity.dsl.description.IntensityCalculationInterval;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL
 * instance.
 *
 * @author Henning Schulz, Alper Hi
 *
 */
public class WessbasPipelineManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasPipelineManager.class);

	private RestTemplate restTemplate;

	private final Path workingDir;

	/**
	 * Constructor.
	 */
	public WessbasPipelineManager(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;

		Path tmpDir;
		try {
			tmpDir = Files.createTempDirectory("wessbas");
		} catch (IOException e) {
			LOGGER.error("Could not create a temp directory!");
			e.printStackTrace();
			tmpDir = Paths.get("wessbas");
		}

		workingDir = tmpDir;

		LOGGER.info("Set working directory to {}", workingDir);
	}

	/**
	 * Runs the pipeline and calls the callback when the model was created.
	 *
	 *
	 * @param task
	 *            Input monitoring data to be transformed into a WESSBAS DSL instance.
	 *
	 * @return The generated workload model.
	 */
	public WessbasBundle runPipeline(TaskDescription task, IntensityCalculationInterval interval) {
		String sessionLogsLink = task.getSource().getSessionLogsLinks().getLink();
		if ("dummy".equals(sessionLogsLink)) {
			return new WessbasBundle(new Date(), WessbasDslInstance.DVDSTORE_PARSED.get());
		}

		SessionLogs sessionLog;
		try {
			sessionLog = restTemplate.getForObject(WebUtils.addProtocolIfMissing(sessionLogsLink), SessionLogs.class);
		} catch (RestClientException e) {
			LOGGER.error("Error when retrieving the session logs!", e);
			return null;
		}
		WorkloadModel workloadModel;
		
		boolean applyModularization = task.getModularizationOptions()!= null && task.getModularizationOptions().getModularizationApproach().equals(ModularizationApproach.WORKLOAD_MODEL);
		
		try {
			if(applyModularization) {
				workloadModel = convertSessionLogIntoWessbasDSLInstanceUsingModularization(sessionLog, task, interval);

			} else {
				workloadModel = convertSessionLogIntoWessbasDSLInstance(sessionLog.getLogs(), interval);

			}
		} catch (Exception e) {
			LOGGER.error("Could not create a WESSBAS workload model!", e);
			workloadModel = null;
		}

		return new WessbasBundle(sessionLog.getDataTimestamp(), workloadModel);
	}

	/**
	 * This method converts a session log into a Wessbas DSL instance.
	 *
	 * @param sessionLog
	 * @throws IOException
	 * @throws GeneratorException
	 * @throws SecurityException
	 */
	private WorkloadModel convertSessionLogIntoWessbasDSLInstance(String sessionLog, IntensityCalculationInterval interval) throws IOException, SecurityException, GeneratorException {
		Path sessionLogsPath = writeSessionLogIntoFile(sessionLog);
		// number of users is calculated based on the given sessions
		Properties intensityProps = createWorkloadIntensity(sessionLog, interval);
		Properties behaviorProps = createBehaviorModel(sessionLogsPath);
		return generateWessbasModel(intensityProps, behaviorProps);
	}
	
	/**
	 * This method converts a session log into a Wessbas DSL instance.
	 *
	 * @param sessionLog
	 * @throws IOException
	 * @throws GeneratorException
	 * @throws SecurityException
	 */
	private WorkloadModel convertSessionLogIntoWessbasDSLInstanceUsingModularization(SessionLogs sessionLogs, TaskDescription task, IntensityCalculationInterval interval) throws IOException, SecurityException, GeneratorException {
		// set 1 as default and configure actual number on demand
		Properties intensityProps = createWorkloadIntensity(sessionLogs.getLogs(), interval);
		
		//Apply Behavior Mix generation
		BehaviorMixManager behaviorManager = new BehaviorMixManager(restTemplate, workingDir);
		SessionsBundlePack sessionsBundles = behaviorManager.runPipeline(sessionLogs);
		
		// Apply Modularization
		WorkloadModularizationManager modularizationManager = new WorkloadModularizationManager(restTemplate);
		BehaviorModelPack behaviorModelPack = new BehaviorModelPack(sessionsBundles, workingDir);
		modularizationManager.runPipeline(task.getTag(), task.getSource(), behaviorModelPack, task.getModularizationOptions().getServices());
		
		Properties behaviorProperties = new Properties();
		behaviorProperties.load(Files.newInputStream(workingDir.resolve("behaviormodelextractor").resolve("behaviormix.txt")));

		return generateWessbasModel(intensityProps, behaviorProperties);
	}

	private Path writeSessionLogIntoFile(String sessionLog) throws IOException {
		Path sessionLogsPath = workingDir.resolve("sessions.dat");
		Files.write(sessionLogsPath, Collections.singletonList(sessionLog), StandardOpenOption.CREATE);
		return sessionLogsPath;
	}

	private Properties createWorkloadIntensity(String sessionLogs,IntensityCalculationInterval interval) throws IOException {
		Properties properties = new Properties();
		properties.put("workloadIntensity.type", "constant");
		
		properties.put("wl.type.value", Integer.toString(calculateIntensity(sessionLogs, interval)));

		properties.store(Files.newOutputStream(workingDir.resolve("workloadIntensity.properties"), StandardOpenOption.CREATE), null);

		return properties;
	}

	private Properties createBehaviorModel(Path sessionLogsPath) throws IOException {
		Path outputDir = workingDir.resolve("behaviormodelextractor");
		outputDir.toFile().mkdir();

		BehaviorModelExtractor behav = new BehaviorModelExtractor();
		behav.createBehaviorModel(sessionLogsPath.toString(), outputDir.toString());

		Properties behaviorProperties = new Properties();
		behaviorProperties.load(Files.newInputStream(workingDir.resolve("behaviormodelextractor").resolve("behaviormix.txt")));
		return behaviorProperties;
	}

	private WorkloadModel generateWessbasModel(Properties workloadIntensityProperties, Properties behaviorModelsProperties) throws FileNotFoundException, SecurityException, GeneratorException {
		M4jdslModelGenerator generator = new M4jdslModelGenerator();
		final String sessionDatFilePath = workingDir.resolve("sessions.dat").toString();

		return generator.generateWorkloadModel(workloadIntensityProperties, behaviorModelsProperties, null, sessionDatFilePath, false);
	}
	
	/**
	 * Calculate intensity based on the parallel session logs. 
	 * @param sessions the session logs
	 * @param interval the used interval/ resolution
	 * @return the intensity which represents the number of users.
	 */
	private int calculateIntensity(String sessionLogsString, IntensityCalculationInterval interval) {
		List<SimplifiedSession> sessions = SimplifiedSessionLogsDeserializer.parse(sessionLogsString);
		IntensityCalculationUtils.sortSessions(sessions);
		long startTime = sessions.get(0).getStartTime();
		
		// The time range for which an intensity will be calculated
		long rangeLength;
		
		if (null == interval) {
			interval = IntensityCalculationInterval.SECOND;
		}
		
		rangeLength = interval.asNumber();
		
		long highestEndTime = 0;
		
		for(SimplifiedSession session: sessions) {
			if(session.getEndTime() > highestEndTime) {
				highestEndTime = session.getEndTime();
			}
		}
		// Check if overall session logs duration is shorter than a single range
		if(rangeLength > (highestEndTime-startTime)) {
			LOGGER.info("The intensity of the given session logs cannot be calculated, because the used range '"+ interval.name()+"' is longer than the overall duration of the session logs. As default numOfthreads is set to 1!");
			return 1;
		}
		
		// rounds highest end time up
		long roundedHighestEndTime = highestEndTime;
		if (highestEndTime % rangeLength != 0) {
			roundedHighestEndTime = (highestEndTime - highestEndTime % rangeLength) + rangeLength;
		}
		
		long completePeriod = roundedHighestEndTime - startTime;
		long amountOfRanges = completePeriod / rangeLength;
		
		ArrayList<Range<Long>> listOfRanges = IntensityCalculationUtils.calculateRanges(startTime, amountOfRanges, rangeLength);
		
		// Remove first and last range from list if necessary
		if(listOfRanges.get(0).getMinimum() != startTime) {
			listOfRanges.remove(0);
		}
		
		if(listOfRanges.get(listOfRanges.size() - 1).getMaximum() != highestEndTime) {
			listOfRanges.remove(listOfRanges.size() - 1);
		}
		
		// This map is used to hold necessary information which will be saved into DB
		List<Integer> intensities = new ArrayList<Integer>();
		
		for(Range<Long> range: listOfRanges) {
			ArrayList<SimplifiedSession> sessionsInRange = new ArrayList<SimplifiedSession>();
			for(SimplifiedSession session: sessions) {
				Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
				if(sessionRange.containsRange(range) || range.contains(session.getStartTime()) 
						|| range.contains(session.getEndTime())) {
					sessionsInRange.add(session);
				}
			}
			int intensityOfRange = (int) IntensityCalculationUtils.calculateIntensityForRange(range, sessionsInRange, rangeLength);		
			intensities.add(intensityOfRange);
		}
		return Math.toIntExact(Math.round(intensities.stream().mapToDouble(a -> a).average().getAsDouble()));
	}

}