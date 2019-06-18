package org.continuity.wessbas.managers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.artifact.SessionLogsInput;
import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.MarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.SubTrace;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.serialization.OPENxtraceSerializationFactory;
import org.spec.research.open.xtrace.dflt.impl.serialization.OPENxtraceSerializationFormat;
import org.spec.research.open.xtrace.dflt.impl.serialization.OPENxtraceSerializer;
import org.springframework.web.client.RestTemplate;

import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.behaviormodelextractor.extraction.ExtractionException;
import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;
import open.xtrace.OPENxtraceUtils;
import wessbas.commons.parser.ParseException;

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

	private static final String SESSION_ID_PREFIX = "generated_";

	private final AtomicLong sessionIdCounter = new AtomicLong(0);

	/**
	 * Eureka rest template
	 */
	private RestTemplate eurekaRestTemplate;

	/**
	 * Plain rest template
	 */
	private RestTemplate plainRestTemplate;

	/**
	 * The current working directory
	 */
	private Path workingDir;

	/**
	 * The csv handler.
	 */
	private CSVHandler csvHandler;

	/**
	 * Constructor
	 *
	 * @param restTemplate
	 *            Eureka rest template
	 */
	public WorkloadModularizationManager(RestTemplate eurekaRestTemplate) {
		this.eurekaRestTemplate = eurekaRestTemplate;
		this.plainRestTemplate = new RestTemplate();
		this.csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

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

	public void runPipeline(String tag, LinkExchangeModel linkExchangeModel, BehaviorModelPack behaviorModelPack, Map<String, String> services) {
		List<SessionsBundle> sessionBundles = behaviorModelPack.getSessionsBundlePack().getSessionsBundles();
		List<HTTPRequestProcessingImpl> httpCallables = OPENxtraceUtils.extractHttpRequestCallables(OPENxtraceUtils.getOPENxtraces(linkExchangeModel, plainRestTemplate));
		Application application = eurekaRestTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl(tag).get(), Application.class);

		MarkovBehaviorModel behaviorModel = new MarkovBehaviorModel();

		for (SessionsBundle sessionBundle : sessionBundles) {
			try {
				behaviorModel.addMarkovChain(modularizeUserGroup(application, sessionBundle, behaviorModelPack, services, httpCallables));
			} catch (IOException e) {
				LOGGER.error("Could not modularize behavior model!", e);
			}
		}

		behaviorModel.synchronizeMarkovChains();

		for (MarkovChain chain : behaviorModel.getMarkovChains()) {
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
	private MarkovChain modularizeUserGroup(Application application, SessionsBundle sessionBundle, BehaviorModelPack behaviorModelPack, Map<String, String> services,
			List<HTTPRequestProcessingImpl> httpCallables) throws FileNotFoundException, IOException {
		LOGGER.info("Modularizing behavior model {} at path {}...", sessionBundle.getBehaviorId(), behaviorModelPack.getPathToBehaviorModelFiles());

		String behaviorFile = behaviorModelPack.getPathToBehaviorModelFiles().resolve("behaviormodelextractor").resolve(FILENAME + sessionBundle.getBehaviorId() + FILE_EXT).toFile().toString();
		MarkovChain markovChain = MarkovChain.fromCsv(csvHandler.readValues(behaviorFile));
		markovChain.setId(FILENAME + sessionBundle.getBehaviorId());

		Map<String, List<Trace>> tracesPerState = getTracesPerState(filterTraces(httpCallables, sessionBundle), application);

		for (String state : markovChain.getRequestStates()) {
			modularizeMarkovState(markovChain, state, tracesPerState.get(state), application, services);
		}

		LOGGER.info("Modularization of {} done.", behaviorModelPack.getPathToBehaviorModelFiles());

		return markovChain;
	}

	private List<HTTPRequestProcessingImpl> filterTraces(List<HTTPRequestProcessingImpl> httpCallables, SessionsBundle sessionBundle) {
		List<String> sessionIds = sessionBundle.getSessions().stream().map(SimplifiedSession::getId).collect(Collectors.toList());

		return httpCallables.stream()
				.filter(p -> p.getHTTPHeaders().get().containsKey("cookie") && sessionIds.contains(OPENxtraceUtils.extractSessionIdFromCookies(p.getHTTPHeaders().get().get("cookie"))))
				.collect(Collectors.toList());
	}

	private void modularizeMarkovState(MarkovChain markovChain, String state, List<Trace> traces, Application application, Map<String, String> services) {
		if ((traces == null) || traces.isEmpty()) {
			LOGGER.info("Keeping state {}.", state);
			return;
		}

		SessionLogs sessionLogs = getModularizedSessionLogs(traces, services);

		if (sessionLogs.getLogs().isEmpty()) {
			LOGGER.info("Removing state {}.", state);
			double[] responseTimeSample = traces.stream().map(Trace::getRoot).map(SubTrace::getRoot).map(HTTPRequestProcessingImpl.class::cast).mapToDouble(r -> r.getResponseTime() / 1000000D)
					.toArray();
			markovChain.removeState(state, NormalDistribution.fromSample(responseTimeSample));
		} else {
			LOGGER.info("Replacing state {}.", state);
			MarkovChain subChain = createSubMarkovChain(state, sessionLogs, markovChain.getId());

			removePrePostProcessingState("PRE_PROCESSING#", subChain);
			removePrePostProcessingState("POST_PROCESSING#", subChain);

			markovChain.replaceState(state, subChain);
		}
	}

	private void removePrePostProcessingState(String prefix, MarkovChain chain) {
		int removed = chain.removeStates(s -> s.startsWith(prefix), NormalDistribution.ZERO);

		if (removed != 1) {
			LOGGER.warn("Expected to remove 1 state with prefix {} but were {} states actually!", prefix, removed);
		}
	}

	private MarkovChain createSubMarkovChain(String rootState, SessionLogs sessionLogs, String behaviorId) {
		if (sessionLogs.getLogs().isEmpty()) {
			return null;
		}

		Path modularizedSessionLogsPath = workingDir.resolve(behaviorId).resolve(rootState);
		modularizedSessionLogsPath.toFile().mkdirs();

		BehaviorModelExtractor extractor = new BehaviorModelExtractor();
		String[][] behaviorModel = null;
		try {
			Files.write(modularizedSessionLogsPath.resolve("sessions.dat"), Collections.singletonList(sessionLogs.getLogs()), StandardOpenOption.CREATE);
			extractor.init(null, null, 0);
			// "simple" will generate a single behavior model (behaviormodel.csv)
			extractor.extract(modularizedSessionLogsPath.resolve("sessions.dat").toString(), modularizedSessionLogsPath.toString(), "simple");
			behaviorModel = csvHandler.readValues(modularizedSessionLogsPath.resolve(FILENAME + FILE_EXT).toFile().toString());

			LOGGER.info("Created behavior model for {}.", rootState);
		} catch (IOException | ExtractionException | ParseException e) {
			LOGGER.error("Could not create behavior model", e);
		}

		return MarkovChain.fromCsv(behaviorModel);
	}

	/**
	 * Retrieves new session logs from session logs service
	 *
	 * @param traces
	 *            the input traces
	 * @param services
	 *            the services, which are going to be targeted
	 * @return {@link SessionLogs}
	 */
	private SessionLogs getModularizedSessionLogs(List<Trace> traces, Map<String, String> services) {
		OPENxtraceSerializer serializer = OPENxtraceSerializationFactory.getInstance().getSerializer(OPENxtraceSerializationFormat.JSON);
		OutputStream stream = new ByteArrayOutputStream();
		serializer.prepare(stream);
		for (Trace trace : traces) {
			serializer.writeTrace(trace);
		}
		serializer.close();

		// Convert the outputstream to a json array
		BufferedReader bufReader = new BufferedReader(new StringReader(stream.toString()));
		String line = null;
		ArrayNode jsonArray = new ArrayNode(JsonNodeFactory.instance);
		ObjectMapper mapper = new ObjectMapper();
		try {
			while ((line = bufReader.readLine()) != null) {
				jsonArray.add(mapper.readTree(line));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		SessionLogsInput input = new SessionLogsInput(services, jsonArray.toString());
		String createSessionLogsLink = RestApi.SessionLogs.CREATE.requestUrl().withQuery(RestApi.SessionLogs.QueryParameters.ADD_PRE_POST_PROCESSING, "true").get();
		return eurekaRestTemplate.postForObject(createSessionLogsLink, input, SessionLogs.class);
	}

	private Map<String, List<Trace>> getTracesPerState(List<HTTPRequestProcessingImpl> filteredCallables, Application application) {
		Map<String, List<Trace>> requestsToReplaceMap = new HashMap<String, List<Trace>>();
		RequestUriMapper uriMapper = new RequestUriMapper(application);

		for (HTTPRequestProcessingImpl httpRequestProcessingImpl : filteredCallables) {
			HttpEndpoint endpoint = uriMapper.map(httpRequestProcessingImpl.getUri(), httpRequestProcessingImpl.getRequestMethod().get().name());

			if (endpoint != null) {
				List<Trace> traces = requestsToReplaceMap.get(endpoint.getId());

				if (traces == null) {
					traces = new ArrayList<>();
					requestsToReplaceMap.put(endpoint.getId(), traces);
				}

				OPENxtraceUtils.setSessionId(httpRequestProcessingImpl, SESSION_ID_PREFIX + sessionIdCounter.getAndIncrement());
				traces.add(httpRequestProcessingImpl.getContainingSubTrace().getContainingTrace());
			}
		}

		return requestsToReplaceMap;
	}

}
