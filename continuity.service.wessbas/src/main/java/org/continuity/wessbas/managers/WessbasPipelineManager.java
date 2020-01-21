package org.continuity.wessbas.managers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.toList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.Range;
import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.entities.artifact.SimplifiedSession;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.NormalDistribution;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovTransition;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.commons.utils.IntensityCalculationUtils;
import org.continuity.commons.utils.SimplifiedSessionLogsDeserializer;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator;
import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL instance.
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
	 * Constructor.
	 */
	public WessbasPipelineManager(RestTemplate restTemplate, Path workingDir) {
		this.restTemplate = restTemplate;
		this.workingDir = workingDir;

		LOGGER.info("Set working directory to {}", workingDir);
	}

	/**
	 * Runs the whole pipeline by creating a behavior model and transforming it into a workload
	 * model.
	 *
	 *
	 * @param task
	 *            Input monitoring data to be transformed into a WESSBAS DSL instance.
	 * @param interval
	 *            The interval for calculating the intensity.
	 *
	 * @see #createBehaviorModelFromSessions(String, VersionOrTimestamp, long)
	 * @see #transformBehaviorModelToWorkloadModelIncludingTailoring(BehaviorModelPack,
	 *      TaskDescription)
	 *
	 * @return The generated workload model.
	 */
	public WessbasBundle runPipeline(TaskDescription task, long interval) {
		String sessionLogsLink = task.getSource().getSessionLinks().getExtendedLink();
		if ("dummy".equals(sessionLogsLink)) {
			return new WessbasBundle(task.getVersion(), WessbasDslInstance.DVDSTORE_PARSED.get());
		}

		WorkloadModel workloadModel;
		try {
			BehaviorModelPack behaviorModelPack = createBehaviorModelFromSessions(task, interval);
			workloadModel = transformBehaviorModelToWorkloadModelIncludingTailoring(behaviorModelPack, task);
		} catch (Exception e) {
			LOGGER.error("Could not create a WESSBAS workload model!", e);
			return null;
		}

		return new WessbasBundle(task.getVersion(), workloadModel);
	}

	/**
	 * Creates a behavior model from (extended) session logs.
	 *
	 * @param task
	 *            The task, which is assumed to hold extended session logs.
	 * @param interval
	 *            The interval for calculating the intensity.
	 * @return The behavior model in a {@link BehaviorModelPack}.
	 * @throws IOException
	 */
	public BehaviorModelPack createBehaviorModelFromSessions(TaskDescription task, long interval) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

		String sessionLogs;
		try {
			sessionLogs = restTemplate.exchange(WebUtils.addProtocolIfMissing(task.getSource().getSessionLinks().getExtendedLink()), HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
		} catch (RestClientException e) {
			LOGGER.error("Error when retrieving the session logs!", e);
			return null;
		}

		createWorkloadIntensity(sessionLogs, interval);

		BehaviorMixManager behaviorManager = new BehaviorMixManager(task.getVersion(), workingDir);
		SessionsBundlePack sessionsBundles = behaviorManager.runPipeline(sessionLogs);

		return new BehaviorModelPack(sessionsBundles, workingDir);
	}

	/**
	 * Creates a behavior model from an externally created {@link MarkovBehaviorModel}.
	 *
	 * @param task
	 *            The task, which is assumed to hold the {@link MarkovBehaviorModel}.
	 * @return The behavior model in a {@link BehaviorModelPack}.
	 * @throws IOException
	 */
	public BehaviorModelPack createBehaviorModelFromMarkovChains(TaskDescription task) throws IOException {
		Path dir = workingDir.resolve("behaviormodelextractor");
		dir.toFile().mkdir();

		MarkovBehaviorModel markovModel = restTemplate.getForObject(WebUtils.addProtocolIfMissing(task.getSource().getBehaviorModelLinks().getLink()), MarkovBehaviorModel.class);

		for (RelativeMarkovChain chain : markovModel.getMarkovChains()) {
			for (String state : chain.getRequestStates()) {
				double maxProb = chain.getTransitions().entrySet().stream().filter(e -> !state.equals(e.getKey())).map(Entry::getValue).map(map -> map.get(state)).filter(Objects::nonNull)
						.mapToDouble(RelativeMarkovTransition::getProbability).max().orElse(0);

				if (maxProb < RelativeMarkovTransition.PRECISION) {
					LOGGER.info("Markov chain {}: removing state {} because it has no incoming transitions larger than {}.", chain.getId(), state, RelativeMarkovTransition.PRECISION);
					chain.removeState(state, NormalDistribution.ZERO);
				}
			}
		}

		markovModel.synchronizeMarkovChains();

		List<ForecastIntensityRecord> intensities = loadIntensities(task.getSource().getIntensity());

		createWorkloadIntensity(intensities);
		writeDummySessionsDat();
		writeUsecases(markovModel, dir);
		updateBehaviorMix(markovModel, intensities);
		writeBehaviorMix(markovModel, dir);
		writeBehaviorModels(markovModel, dir);

		return new BehaviorModelPack(null, workingDir);
	}

	/**
	 * Transforms a behavior model into a workload model and also applies tailoring if requested.
	 *
	 * @param behaviorModelPack
	 *            The behavior model as {@link BehaviorModelPack}.
	 * @param task
	 *            The task describing how to generate the workload model, especially regarding
	 *            tailoring.
	 * @return The workload model.
	 * @throws IOException
	 * @throws SecurityException
	 * @throws GeneratorException
	 */
	public WorkloadModel transformBehaviorModelToWorkloadModelIncludingTailoring(BehaviorModelPack behaviorModelPack, TaskDescription task) throws IOException, SecurityException, GeneratorException {
		boolean applyModularization = (task.getOptions() != null) && (task.getOptions().getTailoringApproach() == TailoringApproach.MODEL_BASED)
				&& TailoringUtils.doTailoring(task.getEffectiveServices());

		if (applyModularization) {
			WorkloadModularizationManager modularizationManager = new WorkloadModularizationManager(restTemplate, task.getAppId(), task.getVersion());
			modularizationManager.runPipeline(task.getVersion(), task.getSource(), behaviorModelPack, task.getEffectiveServices());
		}

		Properties intensityProps = new Properties();
		intensityProps.load(Files.newInputStream(workingDir.resolve("workloadIntensity.properties")));

		Properties behaviorProperties = new Properties();
		behaviorProperties.load(Files.newInputStream(workingDir.resolve("behaviormodelextractor").resolve("behaviormix.txt")));

		return generateWessbasModel(intensityProps, behaviorProperties);
	}

	private List<ForecastIntensityRecord> loadIntensities(String link) {
		if (link == null) {
			return null;
		}

		ForecastIntensityRecord[] records = restTemplate.getForObject(WebUtils.addProtocolIfMissing(link), ForecastIntensityRecord[].class);
		return Arrays.asList(records);
	}

	private Properties createWorkloadIntensity(String sessionLogs, long interval) throws IOException {
		return createWorkloadIntensity(calculateIntensity(sessionLogs, interval));
	}

	private Properties createWorkloadIntensity(List<ForecastIntensityRecord> intensities) throws IOException {
		if ((intensities == null) || (intensities.size() == 0)) {
			LOGGER.warn("Did not get any intensities. Therefore, using the default value 1.");
			return createWorkloadIntensity(1);
		}

		double totalIntensity = intensities.get(0).getContent().values().stream().mapToDouble(x -> x).sum();

		return createWorkloadIntensity((int) Math.round(totalIntensity));
	}

	private Properties createWorkloadIntensity(int intensity) throws IOException {
		Properties properties = new Properties();
		properties.put("workloadIntensity.type", "constant");
		properties.put("wl.type.value", Integer.toString(intensity));

		properties.store(Files.newOutputStream(workingDir.resolve("workloadIntensity.properties"), StandardOpenOption.CREATE), null);

		return properties;
	}

	private void writeUsecases(MarkovBehaviorModel markovModel, Path dir) throws IOException {
		List<String> usecases = markovModel.getMarkovChains().get(0).getRequestStates();
		usecases.add(0, "INITIAL");
		Files.write(dir.resolve("usecases.txt"), usecases);
	}

	private void updateBehaviorMix(MarkovBehaviorModel markovModel, List<ForecastIntensityRecord> intensities) {
		if ((intensities == null) || (intensities.size() == 0)) {
			LOGGER.warn("Did not get any intensities. Therefore, using the default behavior mix.");
			return;
		}

		LOGGER.info("Adjusting the behavior mix based on the intensities...");

		Map<String, Double> absFreq = intensities.stream().map(ForecastIntensityRecord::getContent).flatMap(map -> map.entrySet().stream())
				.collect(groupingBy(Entry::getKey, summingDouble(Entry::getValue)));
		double total = absFreq.values().stream().mapToDouble(x -> x).sum();

		for (RelativeMarkovChain chain : markovModel.getMarkovChains()) {
			chain.setFrequency(absFreq.get(chain.getId()) / total);
		}
	}

	private void writeBehaviorMix(MarkovBehaviorModel markovModel, Path dir) throws IOException {
		List<String> mix = markovModel.getMarkovChains().stream()
				.map(chain -> new StringBuilder().append("gen_behavior_model" + chain.getId()).append("; ").append(dir.resolve(toCsvFile(chain, "gen_behavior_model"))).append("; ")
						.append(chain.getFrequency()).append("; ").append(dir.resolve(toCsvFile(chain, "behaviormodel"))).append(", \\").toString())
				.collect(toList());

		mix.set(0, "behaviorModels = " + mix.get(0));
		String last = mix.get(mix.size() - 1);
		mix.set(mix.size() - 1, last.substring(0, last.length() - 3));

		Files.write(dir.resolve("behaviormix.txt"), mix);
	}

	private void writeBehaviorModels(MarkovBehaviorModel markovModel, Path dir) throws FileNotFoundException, SecurityException, NullPointerException, IOException {
		CSVHandler csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

		for (RelativeMarkovChain chain : markovModel.getMarkovChains()) {
			csvHandler.writeValues(dir.resolve(toCsvFile(chain, "behaviormodel")).toString(), chain.toCsv());
		}
	}

	private String toCsvFile(RelativeMarkovChain chain, String filePrefix) {
		return new StringBuilder().append(filePrefix).append(chain.getId()).append(".csv").toString();
	}

	private void writeDummySessionsDat() throws IOException {
		List<String> content = Collections.singletonList("SID;\"ID\":0:0");
		Files.write(workingDir.resolve("sessions.dat"), content);
	}

	private WorkloadModel generateWessbasModel(Properties workloadIntensityProperties, Properties behaviorModelsProperties) throws FileNotFoundException, SecurityException, GeneratorException {
		M4jdslModelGenerator generator = new M4jdslModelGenerator();
		final String sessionDatFilePath = workingDir.resolve("sessions.dat").toString();

		return generator.generateWorkloadModel(workloadIntensityProperties, behaviorModelsProperties, null, sessionDatFilePath, false);
	}

	/**
	 * Calculate intensity based on the parallel session logs.
	 *
	 * @param sessions
	 *            the session logs
	 * @param interval
	 *            the used interval/ resolution
	 * @return the intensity which represents the number of users.
	 */
	private int calculateIntensity(String sessionLogsString, long interval) {
		List<SimplifiedSession> sessions = SimplifiedSessionLogsDeserializer.parse(sessionLogsString);
		IntensityCalculationUtils.sortSessions(sessions);
		long startTime = sessions.get(0).getStartTime();

		long highestEndTime = 0;

		for (SimplifiedSession session : sessions) {
			if (session.getEndTime() > highestEndTime) {
				highestEndTime = session.getEndTime();
			}
		}

		// The time range for which an intensity will be calculated
		long rangeLength = Math.min(interval, highestEndTime - startTime);

		// rounds highest end time up
		long roundedHighestEndTime = highestEndTime;
		if ((highestEndTime % rangeLength) != 0) {
			roundedHighestEndTime = (highestEndTime - (highestEndTime % rangeLength)) + rangeLength;
		}

		long completePeriod = roundedHighestEndTime - startTime;
		long amountOfRanges = completePeriod / rangeLength;

		ArrayList<Range<Long>> listOfRanges = IntensityCalculationUtils.calculateRanges(startTime, amountOfRanges, rangeLength);

		// Remove first and last range from list if necessary
		if (listOfRanges.get(0).getMinimum() != startTime) {
			listOfRanges.remove(0);
		}

		if (listOfRanges.get(listOfRanges.size() - 1).getMaximum() != highestEndTime) {
			listOfRanges.remove(listOfRanges.size() - 1);
		}

		// This map is used to hold necessary information which will be saved into DB
		List<Integer> intensities = new ArrayList<Integer>();

		for (Range<Long> range : listOfRanges) {
			ArrayList<SimplifiedSession> sessionsInRange = new ArrayList<SimplifiedSession>();
			for (SimplifiedSession session : sessions) {
				Range<Long> sessionRange = Range.between(session.getStartTime(), session.getEndTime());
				if (sessionRange.containsRange(range) || range.contains(session.getStartTime()) || range.contains(session.getEndTime())) {
					sessionsInRange.add(session);
				}
			}
			int intensityOfRange = (int) IntensityCalculationUtils.calculateIntensityForRange(range, sessionsInRange, rangeLength);
			intensities.add(intensityOfRange);
		}

		// TODO: use list as varying intensity?

		return Math.toIntExact(Math.round(intensities.stream().mapToDouble(a -> a).average().getAsDouble()));
	}

}