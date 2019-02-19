package org.continuity.wessbas.managers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.continuity.api.entities.artifact.BehaviorModel.Behavior;
import org.continuity.api.entities.artifact.BehaviorModel.MarkovState;
import org.continuity.api.entities.artifact.BehaviorModel.Transition;
import org.continuity.api.entities.artifact.ModularizedSessionLogs;
import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.artifact.SessionLogsInput;
import org.continuity.api.entities.artifact.SessionsBundle;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.api.entities.deserialization.BehaviorModelSerializer;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Manager which modularizes the Behavior Models
 *
 * @author Tobias Angerstein
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
		Application application = eurekaRestTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(tag).get(), Application.class);
		for (SessionsBundle sessionBundle : sessionBundles) {
			modularizeUserGroup(application, sessionBundle, behaviorModelPack, services, httpCallables);
		}
	}

	/**
	 * Modularizes a certain behavior model
	 *
	 * @param sessionBundle
	 *            The session bundle which is modularized
	 * @param services
	 *            The targeted load test of the modularized services.
	 */
	private void modularizeUserGroup(Application application, SessionsBundle sessionBundle, BehaviorModelPack behaviorModelPack, Map<String, String> services,
			List<HTTPRequestProcessingImpl> httpCallables) {
		// Get all sessionIds
		List<String> sessionIds = sessionBundle.getSessions().stream().map(SimplifiedSession::getId).collect(Collectors.toList());

		// Filter callables, which match with the sessionIds
		// Henning: Das was getan wird auslagern Methode auslagern
		List<HTTPRequestProcessingImpl> filteredCallables = httpCallables.stream()
				.filter(p -> p.getHTTPHeaders().get().containsKey("cookie") && sessionIds.contains(OPENxtraceUtils.extractSessionIdFromCookies(p.getHTTPHeaders().get().get("cookie"))))
				.collect(Collectors.toList());
		Behavior rootBehaviorModel = null;

		try {
			rootBehaviorModel = BehaviorModelSerializer.deserializeBehaviorModel(csvHandler
					.readValues(behaviorModelPack.getPathToBehaviorModelFiles().resolve("behaviormodelextractor").resolve(FILENAME + sessionBundle.getBehaviorId() + FILE_EXT).toFile().toString()));
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}

		List<String> markovStateNames = rootBehaviorModel.getMarkovStates().stream().map(p -> p.getId()).collect(Collectors.toList());

		// Delete unnecessary endpoints in the application model, which are not represented in the
		// markov chain.
		deleteNotOccurringEndpoints(application, markovStateNames);

		// Get Replacing Requests per Markov chain
		// TODO: !!! multiple traces with the same session ID will be put into the same session
		Map<String, List<Trace>> requestsToReplaceMap = getReplacingRequests(filteredCallables, application, services);

		// Get modularized Session logs for each markov state
		Map<String, ModularizedSessionLogs> modularizedSessionLogsMap = requestsToReplaceMap.entrySet().stream()
				.map(p -> new AbstractMap.SimpleEntry<String, ModularizedSessionLogs>(p.getKey(), getModularizedSessionLogs(p.getValue(), services))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// TODO: Schreibe gemergte Sessionlogs in anderes File
		// Rename usecases of each session logs to {markovStateName}#useCase
		modularizedSessionLogsMap.entrySet().stream().forEach(e -> e.setValue(renameSessionLogs(e)));

		// Write modularized session logs back to session logs file
		writeSessionLogs(modularizedSessionLogsMap, behaviorModelPack.getPathToBehaviorModelFiles());

		// Get behavior model for each modularized session log
		Map<String, Pair<Behavior, ModularizedSessionLogs>> modularizedBehaviorModelsPerMarkovState = new HashMap<String, Pair<Behavior, ModularizedSessionLogs>>();
		for (Entry<String, ModularizedSessionLogs> entry : modularizedSessionLogsMap.entrySet()) {
			Behavior behavior = BehaviorModelSerializer.deserializeBehaviorModel(getBehaviorModel(entry.getValue(), entry.getKey(), sessionBundle.getBehaviorId()));
			modularizedBehaviorModelsPerMarkovState.put(entry.getKey(), Pair.of(behavior, entry.getValue()));
		}

		// Merge subbehavioralModels in the current behavior model
		new BehaviorModelMerger().replaceMarkovStatesWithSubMarkovChains(rootBehaviorModel, modularizedBehaviorModelsPerMarkovState);

		// Write behavior model back to file
		try {
			csvHandler.writeValues(behaviorModelPack.getPathToBehaviorModelFiles().resolve("behaviormodelextractor").resolve(FILENAME + sessionBundle.getBehaviorId() + FILE_EXT).toFile().toString(),
					BehaviorModelSerializer.serializeBehaviorModel(rootBehaviorModel));
		} catch (SecurityException | NullPointerException | IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Writes the modularized session logs in the existing session logs file
	 *
	 * @param modularizedSessionLogsMap
	 *            the modularized session logs
	 * @param path
	 *            the path to the session logs directory
	 */
	private void writeSessionLogs(Map<String, ModularizedSessionLogs> modularizedSessionLogsMap, Path path) {
		String rootSessionLogsString = "";
		try {
			rootSessionLogsString = FileUtils.readFileToString(path.resolve("sessions.dat").toFile(), "UTF-8");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Map<String, String> rootSessionLogs = Stream.of(rootSessionLogsString.split("\n")).map(n -> new AbstractMap.SimpleEntry<>(n.split(";")[0], n))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		for (SessionLogs modularizedSessionLogs : modularizedSessionLogsMap.values()) {
			if (modularizedSessionLogs.getLogs().isEmpty()) {
				continue;
			}
			for (String modularizedSessionLog : modularizedSessionLogs.getLogs().split("\n")) {
				String[] modularizedRequests = modularizedSessionLog.split(";");
				if (rootSessionLogs.containsKey(modularizedRequests[0])) {
					// Add all modularized requests to the corresponding session log
					rootSessionLogs.put(modularizedRequests[0],
							rootSessionLogs.get(modularizedRequests[0]).concat(";").concat(String.join(";", Arrays.copyOfRange(modularizedRequests, 1, modularizedRequests.length))));
				}
			}
		}
		// Write back into String
		rootSessionLogsString = String.join("\n", rootSessionLogs.values().toArray(new String[rootSessionLogs.size()]));

		// Write back into File
		try {
			FileUtils.writeStringToFile(path.resolve("sessions.dat").toFile(), rootSessionLogsString, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Adds a prefix to each use case
	 *
	 * @param entry
	 *            {@link Entry<String, SessionLogs>} containing the modularized session logs of a
	 *            certain markov state.
	 * @return the session logs containing the renamed use case names.
	 */
	private ModularizedSessionLogs renameSessionLogs(Entry<String, ModularizedSessionLogs> entry) {
		if (entry.getValue().getLogs().isEmpty()) {
			return entry.getValue();
		}
		ModularizedSessionLogs sessionLogsContainer = entry.getValue();
		String[] sessionLogs = sessionLogsContainer.getLogs().split("\n");
		for (int i = 0; i < sessionLogs.length; i++) {
			String[] requests = sessionLogs[i].split(";");
			for (int j = 0; j < requests.length; j++) {
				String[] requestParameter = requests[j].split(":");
				// skip session id
				if (requestParameter.length == 1) {
					continue;
				}
				requestParameter[0] = "\"" + entry.getKey() + STATE_NAME_LIMITER + requestParameter[0].substring(1, requestParameter[0].length() - 1) + "\"";
				requests[j] = String.join(":", requestParameter);
			}
			sessionLogs[i] = String.join(";", requests);
		}
		String sessionLogsString = String.join("\n", sessionLogs);
		sessionLogsContainer.setLogs(sessionLogsString);
		return sessionLogsContainer;
	}

	/**
	 * Behavior model for the provided session log.
	 *
	 * @param sessionLogs
	 *            the modularized session logs
	 * @param markovChain
	 *            the markov chain
	 * @param behaviorId
	 *            the behavior id
	 * @return
	 */
	private String[][] getBehaviorModel(SessionLogs sessionLogs, String markovChain, int behaviorId) {
		if (sessionLogs.getLogs().isEmpty()) {
			return null;
		}
		Path modularizedSessionLogsPath = workingDir.resolve(String.valueOf(behaviorId));
		modularizedSessionLogsPath.toFile().mkdir();
		modularizedSessionLogsPath = modularizedSessionLogsPath.resolve(markovChain);
		modularizedSessionLogsPath.toFile().mkdir();

		moveEachRequestToSeparateSessionLog(sessionLogs); // TODO: Why ???
		BehaviorModelExtractor extractor = new BehaviorModelExtractor();
		String[][] behaviorModel = null;
		try {
			Files.write(modularizedSessionLogsPath.resolve("sessions.dat"), Collections.singletonList(sessionLogs.getLogs()), StandardOpenOption.CREATE);
			extractor.init(null, null, 0);
			// TODO: The sessions will be clustered
			extractor.createBehaviorModel(modularizedSessionLogsPath.resolve("sessions.dat").toString(), modularizedSessionLogsPath.toString());
			// TODO: replace with
			// extractor.extract(modularizedSessionLogsPath.resolve("sessions.dat").toString(),
			// modularizedSessionLogsPath.toString(), "none");
			behaviorModel = csvHandler.readValues(modularizedSessionLogsPath.resolve(FILENAME + "0" + FILE_EXT).toFile().toString());
		} catch (NullPointerException | IOException | ExtractionException e) {
			e.printStackTrace();
		}
		return behaviorModel;
	}

	/**
	 * Writes each request as separated session log.
	 * @param sessionLogs
	 */
	private void moveEachRequestToSeparateSessionLog(SessionLogs sessionLogsContainer) {
		ArrayList<String> separatedRequests = new ArrayList<String>();
		String[] sessionLogs = sessionLogsContainer.getLogs().split("\n");
		for (int i = 0; i < sessionLogs.length; i++) {
			String[] requests = sessionLogs[i].split(";");
			for (int j = 1; j < requests.length; j++) {
				// TODO: !!! conflicts, e.g., i=1, j=23 and i=12, j=3
				separatedRequests.add("fakeId" + i + j + ";" + requests[j]);
			}
		}
		sessionLogsContainer.setLogs(String.join("\n", separatedRequests.toArray(new String[separatedRequests.size()])));
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
	private ModularizedSessionLogs getModularizedSessionLogs(List<Trace> traces, Map<String, String> services) {
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
		String createSessionLogsLink = RestApi.SessionLogs.CREATE.requestUrl().get();
		return eurekaRestTemplate.postForObject(createSessionLogsLink, input, ModularizedSessionLogs.class);
	}

	/**
	 * Returns all requests, which need to be replaced
	 *
	 * @param filteredCallables
	 *            All Callables, which are part of a specific behavior model
	 * @param application
	 *            the application model, which only contains the endpoints, which are represented in
	 *            the behavior model.
	 * @param services
	 *            The service(s) under test.
	 * @return
	 */
	//TODO: Muss auch ohne Angabe vom service hostname gehen
	private Map<String, List<Trace>> getReplacingRequests(List<HTTPRequestProcessingImpl> filteredCallables, Application application, Map<String, String> services) {
		Map<String, List<Trace>> requestsToReplaceMap = new HashMap<String, List<Trace>>();
		RequestUriMapper uriMapper = new RequestUriMapper(application);
		for (HTTPRequestProcessingImpl httpRequestProcessingImpl : filteredCallables) {
			HttpEndpoint endpoint = uriMapper.map(httpRequestProcessingImpl.getUri(), httpRequestProcessingImpl.getRequestMethod().get().name());
			if (!services.values().contains(endpoint.getDomain())) {
				// This request is needed to be modularized by the session logs service
				if (requestsToReplaceMap.containsKey(endpoint.getId())) {
					requestsToReplaceMap.get(endpoint.getId()).add(httpRequestProcessingImpl.getContainingSubTrace().getContainingTrace());
				} else {
					requestsToReplaceMap.put(endpoint.getId(), new ArrayList<Trace>(Arrays.asList(httpRequestProcessingImpl.getContainingSubTrace().getContainingTrace())));
				}
			}
		}
		return requestsToReplaceMap;
	}

	/**
	 * Deletes all {@link Endpoint} objects, which do not occur in the current Markov chain.
	 *
	 * @param application
	 *            the application model
	 * @param markovStateNames
	 *            all markov state names
	 */
	private void deleteNotOccurringEndpoints(Application application, List<String> markovStateNames) {
		List<Endpoint<?>> endpointsToDelete = new ArrayList<Endpoint<?>>();
		for (Endpoint<?> endpoint : application.getEndpoints()) {
			if (!markovStateNames.contains(endpoint.getId())) {
				endpointsToDelete.add(endpoint);
			}
		}
		application.getEndpoints().removeAll(endpointsToDelete);
	}

	private boolean validateMarkovState(MarkovState markovState) {
		if (markovState.getTransitions().isEmpty()) {
			return true;
		}
		double propability = 0;
		for (Transition transition : markovState.getTransitions()) {
			propability += transition.getProbability();
		}
		return Math.abs(propability - 1.0) < 0.00000001;
	}
}
