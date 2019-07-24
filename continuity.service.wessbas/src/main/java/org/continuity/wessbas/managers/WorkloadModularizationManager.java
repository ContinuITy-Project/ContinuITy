package org.continuity.wessbas.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.config.SessionTailoringDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * Manager which modularizes the Behavior Models
 *
 * @author Tobias Angerstein, Henning Schulz
 *
 */
public class WorkloadModularizationManager {

	/**
	 * Exit state name
	 */
	static final String EXIT_STATE_NAME = "$";

	/**
	 * Initial state name
	 */
	static final String INITIAL_STATE_NAME = "INITIAL*";

	/**
	 * Limiter between root state name and sub state name
	 */
	static final String STATE_NAME_LIMITER = "#";

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadModularizationManager.class);

	/**
	 * Filename of the behaviormodel
	 */
	private static final String FILENAME = "behaviormodel";

	/**
	 * File extension
	 */
	private static final String FILE_EXT = ".csv";

	/**
	 * Eureka rest template
	 */
	private RestTemplate eurekaRestTemplate;

	/**
	 * The current working directory
	 */
	private Path workingDir;

	/**
	 * The csv handler.
	 */
	private CSVHandler csvHandler;

	private final AppId aid;

	private final VersionOrTimestamp version;

	/**
	 * Constructor
	 *
	 * @param restTemplate
	 *            Eureka rest template
	 */
	public WorkloadModularizationManager(RestTemplate eurekaRestTemplate, AppId aid, VersionOrTimestamp version) {
		this.eurekaRestTemplate = eurekaRestTemplate;
		this.csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);
		this.aid = aid;
		this.version = version;

		Path tmpDir;
		try {
			tmpDir = Files.createTempDirectory("wessbas-modularization");
		} catch (IOException e) {
			LOGGER.error("Could not create a temp directory!");
			e.printStackTrace();
			tmpDir = Paths.get("wessbas-modularization");
		}

		workingDir = tmpDir;

		LOGGER.info("Set working directory to {}", workingDir);
	}

	public void runPipeline(VersionOrTimestamp version, LinkExchangeModel linkExchangeModel, BehaviorModelPack behaviorModelPack, Map<AppId, String> services) {
		List<SessionsBundle> sessionBundles = behaviorModelPack.getSessionsBundlePack().getSessionsBundles();

		MarkovBehaviorModel behaviorModel = new MarkovBehaviorModel();

		for (SessionsBundle sessionBundle : sessionBundles) {
			try {
				behaviorModel.addMarkovChain(modularizeUserGroup(sessionBundle, behaviorModelPack, services));
			} catch (IOException e) {
				LOGGER.error("Could not modularize behavior model!", e);
			}
		}

		behaviorModel.synchronizeMarkovChains();

		for (RelativeMarkovChain chain : behaviorModel.getMarkovChains()) {
			String behaviorFile = behaviorModelPack.getPathToBehaviorModelFiles().resolve("behaviormodelextractor").resolve(chain.getId() + FILE_EXT).toFile().toString();
			LOGGER.info("Storing the modularized behavior model to {}...", behaviorFile);

			try {
				csvHandler.writeValues(behaviorFile, chain.toCsv());
			} catch (SecurityException | NullPointerException | IOException e) {
				LOGGER.error("Could not save the behavior model!", e);
			}
		}
	}

	/**
	 * Modularizes a certain behavior model. That is, each Markov state representing a request that
	 * is not in the set of tested services is replaced by several other states representing the
	 * behavior that this request causes at the target services.
	 *
	 * @param sessionBundle
	 *            The session bundle which is modularized
	 * @param services
	 *            The targeted load test of the modularized services.
	 * @return The modularized Markov chain.
	 * @throws IOException
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 */
	private RelativeMarkovChain modularizeUserGroup(SessionsBundle sessionBundle, BehaviorModelPack behaviorModelPack, Map<AppId, String> serviceMap) throws FileNotFoundException, IOException {
		LOGGER.info("Modularizing behavior model {} at path {}...", sessionBundle.getBehaviorId(), behaviorModelPack.getPathToBehaviorModelFiles());

		String behaviorFile = behaviorModelPack.getPathToBehaviorModelFiles().resolve("behaviormodelextractor").resolve(FILENAME + sessionBundle.getBehaviorId() + FILE_EXT).toFile().toString();
		RelativeMarkovChain markovChain = RelativeMarkovChain.fromCsv(csvHandler.readValues(behaviorFile));
		markovChain.setId(FILENAME + sessionBundle.getBehaviorId());

		List<String> services = serviceMap.keySet().stream().map(AppId::getService).collect(Collectors.toList());

		for (String state : markovChain.getRequestStates()) {
			RelativeMarkovChain subChain = retrieveSubChain(aid, state, version, services, sessionBundle);
			modularizeMarkovState(markovChain, state, subChain);
		}

		LOGGER.info("Modularization of {} done.", behaviorModelPack.getPathToBehaviorModelFiles());

		return markovChain;
	}

	private RelativeMarkovChain retrieveSubChain(AppId aid, String state, VersionOrTimestamp version, List<String> services, SessionsBundle sessionBundle) {
		SessionTailoringDescription description = createTailoringDescription(aid, state, version, services, sessionBundle);

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<SessionTailoringDescription> entity = new HttpEntity<SessionTailoringDescription>(description, headers);

		ResponseEntity<RelativeMarkovChain> response;
		try {
			response = eurekaRestTemplate.exchange(RestApi.Cobra.BehaviorModel.CREATE.requestUrl().get(), HttpMethod.POST, entity, RelativeMarkovChain.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not retrieve tailored Markov chain!", e);
			LOGGER.warn("Ignoring tailoring of state {}.", state);
			return null;
		}

		return response.getBody();
	}

	private SessionTailoringDescription createTailoringDescription(AppId aid, String state, VersionOrTimestamp version, List<String> services, SessionsBundle sessionBundle) {
		List<String> sessionIds = sessionBundle.getSessions().stream().map(SimplifiedSession::getId).collect(Collectors.toList());

		SessionTailoringDescription description = new SessionTailoringDescription();

		description.setAid(aid);
		description.setRootEndpoint(state);
		description.setVersion(version);
		description.setTailoring(services);
		description.setIncludePrePostProcessing(true);
		description.setSessionIds(sessionIds);

		return description;
	}

	private void modularizeMarkovState(RelativeMarkovChain markovChain, String state, RelativeMarkovChain subChain) {
		if ((subChain == null) || ((subChain.getNumberOfRequestStates() == 1) && subChain.getRequestStates().contains(state))) {
			LOGGER.info("Keeping state {}.", state);
			return;
		}

		if (subChain.getNumberOfRequestStates() == 0) {
			LOGGER.info("Removing state {}.", state);
			NormalDistribution responseTime = subChain.getTransition(RelativeMarkovChain.INITIAL_STATE, RelativeMarkovChain.FINAL_STATE).getThinkTime();
			markovChain.removeState(state, responseTime);
		} else {
			LOGGER.info("Replacing state {}.", state);
			markovChain.replaceState(state, subChain);
		}
	}

}
