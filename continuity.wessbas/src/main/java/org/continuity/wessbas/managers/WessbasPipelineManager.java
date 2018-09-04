package org.continuity.wessbas.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.commons.utils.WebUtils;
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
	public WessbasBundle runPipeline(String sessionLogsLink) {
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

		try {
			workloadModel = convertSessionLogIntoWessbasDSLInstance(sessionLog.getLogs());
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
	private WorkloadModel convertSessionLogIntoWessbasDSLInstance(String sessionLog) throws IOException, SecurityException, GeneratorException {
		Path sessionLogsPath = writeSessionLogIntoFile(sessionLog);
		// set 1 as default and configure actual number on demand
		Properties intensityProps = createWorkloadIntensity(1);
		Properties behaviorProps = createBehaviorModel(sessionLogsPath);
		return generateWessbasModel(intensityProps, behaviorProps);
	}

	private Path writeSessionLogIntoFile(String sessionLog) throws IOException {
		Path sessionLogsPath = workingDir.resolve("sessions.dat");
		Files.write(sessionLogsPath, Collections.singletonList(sessionLog), StandardOpenOption.CREATE);
		return sessionLogsPath;
	}

	private Properties createWorkloadIntensity(int numberOfUsers) throws IOException {
		Properties properties = new Properties();
		properties.put("workloadIntensity.type", "constant");
		properties.put("wl.type.value", Integer.toString(numberOfUsers));

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

}