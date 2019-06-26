package org.continuity.wessbas.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.commons.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import net.sf.markov4jmeter.behavior.BehaviorMix;
import net.sf.markov4jmeter.behavior.Session;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.ExtractionException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import wessbas.commons.parser.ParseException;

/**
 * 
 * @author Alper Hidiroglu
 *
 */
public class BehaviorMixManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorMixManager.class);

	private RestTemplate restTemplate;

	private final Path workingDir;

	public Path getWorkingDir() {
		return workingDir;
	}

	/**
	 * Constructor.
	 */
	public BehaviorMixManager(RestTemplate restTemplate) {
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
	 * Constructor.
	 */
	public BehaviorMixManager(RestTemplate restTemplate, Path workingDir) {
		this.restTemplate = restTemplate;
		this.workingDir = workingDir;
	}

	/**
	 * Runs the pipeline and returns a SessionsBundlePack that holds a list of SessionBundles.
	 *
	 *
	 * @param task
	 *            Input monitoring data to be transformed into a WESSBAS DSL instance.
	 *
	 * @return The generated workload model.
	 */
	public SessionsBundlePack runPipeline(String sessionLogsLink) {

		SessionLogs sessionLog;
		try {
			sessionLog = restTemplate.getForObject(WebUtils.addProtocolIfMissing(sessionLogsLink), SessionLogs.class);
		} catch (RestClientException e) {
			LOGGER.error("Error when retrieving the session logs!", e);
			return null;
		}
		BehaviorMix mix;
		SessionsBundlePack sessionsBundles;

		try {
			mix = convertSessionLogIntoBehaviorMix(sessionLog.getLogs());
			sessionsBundles = extractSessions(sessionLog.getDataTimestamp(), mix);

		} catch (Exception e) {
			LOGGER.error("Could not create the Behavior Mix!", e);
			mix = null;
			sessionsBundles = null;
		}

		return sessionsBundles;
	}

	/**
	 * Runs the pipeline and returns a SessionsBundlePack that holds a list of SessionBundles.
	 * 
	 * @param sessionLogs
	 *            the {@link SessionLogs}
	 * @return the session logs
	 */
	public SessionsBundlePack runPipeline(SessionLogs sessionLogs) {
		BehaviorMix mix;
		SessionsBundlePack sessionsBundles;

		try {
			mix = convertSessionLogIntoBehaviorMix(sessionLogs.getLogs());
			sessionsBundles = extractSessions(sessionLogs.getDataTimestamp(), mix);

		} catch (Exception e) {
			LOGGER.error("Could not create the Behavior Mix!", e);
			mix = null;
			sessionsBundles = null;
		}

		return sessionsBundles;
	}

	/**
	 * This method extracts the Behavior Mix from a session log.
	 *
	 * @param sessionLog
	 * @throws IOException
	 * @throws GeneratorException
	 * @throws SecurityException
	 */
	private BehaviorMix convertSessionLogIntoBehaviorMix(String sessionLog) throws IOException, SecurityException, GeneratorException, ExtractionException, ParseException {
		Path sessionLogsPath = writeSessionLogIntoFile(sessionLog);
		BehaviorMix mix = createBehaviorMix(sessionLogsPath);
		return mix;
	}

	/**
	 * 
	 * @param sessionLog
	 * @return
	 * @throws IOException
	 */
	private Path writeSessionLogIntoFile(String sessionLog) throws IOException {
		Path sessionLogsPath = workingDir.resolve("sessions.dat");
		Files.write(sessionLogsPath, Collections.singletonList(sessionLog), StandardOpenOption.CREATE);
		return sessionLogsPath;
	}

	/**
	 * Creates the Behavior Mix and writes the corresponding files.
	 * 
	 * @param sessionLogsPath
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws ExtractionException
	 */
	private BehaviorMix createBehaviorMix(Path sessionLogsPath) throws IOException, ParseException, ExtractionException {
		Path outputDir = workingDir.resolve("behaviormodelextractor");
		outputDir.toFile().mkdir();

		BehaviorModelExtractor extractor = new BehaviorModelExtractor();
		extractor.init(null, null, 0);
		BehaviorMix mix = extractor.extractBehaviorMix(sessionLogsPath.toString(), outputDir.toString());

		extractor.writeIntoFiles(mix, outputDir.toString());

		return mix;
	}

	/**
	 * 
	 * @param mix
	 * @return
	 */
	private SessionsBundlePack extractSessions(Date timestamp, BehaviorMix mix) {
		SessionsBundlePack sessionsBundles = new SessionsBundlePack(timestamp, new LinkedList<SessionsBundle>());
		for (int i = 0; i < mix.getEntries().size(); i++) {
			List<SimplifiedSession> simplifiedSessions = new LinkedList<SimplifiedSession>();
			for (Session session : mix.getEntries().get(i).getSessions()) {
				SimplifiedSession simpleSession = new SimplifiedSession(session.getId(), session.getStartTime(), session.getEndTime());
				simplifiedSessions.add(simpleSession);
			}
			SessionsBundle sessBundle = new SessionsBundle(i, simplifiedSessions);
			sessionsBundles.getSessionsBundles().add(sessBundle);
		}
		return sessionsBundles;
	}

}
