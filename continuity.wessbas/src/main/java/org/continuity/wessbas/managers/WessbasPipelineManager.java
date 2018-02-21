package org.continuity.wessbas.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;

import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private final Consumer<WorkloadModel> onModelCreatedCallback;

	private RestTemplate restTemplate;

	private final Path workingDir;

	/**
	 * Constructor.
	 *
	 * @param onModelCreatedCallback
	 *            The function to be called when the model was created.
	 */
	public WessbasPipelineManager(Consumer<WorkloadModel> onModelCreatedCallback, RestTemplate restTemplate) {
		this.onModelCreatedCallback = onModelCreatedCallback;
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
	 * @param data
	 *            Input monitoring data to be transformed into a WESSBAS DSL
	 *            instance.
	 */
	public void runPipeline(MonitoringData data) {
		if ("dummy".equals(data.getDataLink())) {
			onModelCreatedCallback.accept(WessbasDslInstance.DVDSTORE_PARSED.get());
			return;
		}

		String sessionLog = getSessionLog(data);
		WorkloadModel workloadModel;

		try {
			workloadModel = convertSessionLogIntoWessbasDSLInstance(sessionLog);
		} catch (SecurityException | IOException | GeneratorException e) {
			LOGGER.error("Could not create a WESSBAS workload model!");
			e.printStackTrace();
			workloadModel = null;
		}

		onModelCreatedCallback.accept(workloadModel);
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

	/**
	 * Sends request to Session Logs webservice and gets Session Log
	 *
	 * @param data
	 * @return
	 */
	public String getSessionLog(MonitoringData data) {
		String urlString = "http://session-logs?link=";
		try {
			urlString += URLEncoder.encode(data.getDataLink(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Error during URL encoding!", e);
			return null;
		}

		String tag = extractTag(data.getStorageLink());
		if (tag != null) {
			urlString += "&tag=" + tag;
		}

		String sessionLog = this.restTemplate.getForObject(urlString, String.class);

		LOGGER.info("Got session logs: {}", sessionLog);

		return sessionLog;
	}

	private String extractTag(String storageLink) {
		String storageId = extractStorageId(storageLink);

		if (storageId == null) {
			return null;
		} else {
			return storageId.substring(0, storageId.lastIndexOf("-"));
		}
	}

	private String extractStorageId(String storageLink) {
		String[] tokens = storageLink.split("/");

		if (tokens.length != 3) {
			LOGGER.error("Illegal storage link format: {}", storageLink);
			return null;
		} else if (!tokens[2].matches(".+-\\d+")) {
			LOGGER.error("Illegal storage link format: {}", storageLink);
			return null;
		} else {
			return tokens[2];
		}
	}

}