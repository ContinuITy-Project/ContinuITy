package org.continuity.wessbas.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.commons.utils.WebUtils;
import org.continuity.wessbas.entities.WessbasBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator;
import wessbas.commons.util.XmiEcoreHandler;

/**
 * Manages the workload model pipeline from the input data to the output WESSBAS DSL
 * instance.
 *
 * @author Alper Hidiroglu
 *
 */
public class WorkloadModelManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(WessbasPipelineManager.class);

	private RestTemplate restTemplate;
	
	private Path workingDir;

	/**
	 * Constructor.
	 */
	public WorkloadModelManager(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
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
	public WessbasBundle runPipeline(String forecastLink, Path pathToFiles) {
		
		ForecastBundle forecastBundle = restTemplate.getForObject(WebUtils.addProtocolIfMissing(forecastLink), ForecastBundle.class);
		
		this.workingDir = pathToFiles;
		
		LOGGER.info("Working directory is {}", workingDir);

		WorkloadModel workloadModel;

		try {
			workloadModel = generateWessbasDSLInstance(forecastBundle);
		} catch (Exception e) {
			LOGGER.error("Could not create a WESSBAS workload model!", e);
			workloadModel = null;
		}

		return new WessbasBundle(forecastBundle.getTimestamp(), workloadModel);
	}

	/**
	 * This method generates a Wessbas DSL instance.
	 *
	 * @param sessionLog
	 * @throws IOException
	 * @throws GeneratorException
	 * @throws SecurityException
	 */
	private WorkloadModel generateWessbasDSLInstance(ForecastBundle forecastBundle) throws IOException, SecurityException, GeneratorException {
		// set 1 as default and configure actual number on demand
		Properties intensityProps = createWorkloadIntensity(forecastBundle.getWorkloadIntensity());
		Properties behaviorProps = loadBehaviorMix(forecastBundle);
		WorkloadModel workloadModel = generateWessbasModel(intensityProps, behaviorProps);
		// update the behavior mix
		for(int i = 0; i < forecastBundle.getProbabilities().size(); i++) {
			workloadModel.getBehaviorMix().getRelativeFrequencies().get(i).setValue(forecastBundle.getProbabilities().get(i));
		}

		final String xmiOutputFilePath = "workloadmodel/workloadmodel.xmi";
		XmiEcoreHandler.getInstance().ecoreToXMI(workloadModel, xmiOutputFilePath);
		return workloadModel;
	}

	private Properties loadBehaviorMix(ForecastBundle forecastBundle) throws IOException {
		Properties behaviorProperties = new Properties();
		behaviorProperties.load(Files.newInputStream(workingDir.resolve("behaviormodelextractor").resolve("behaviormix.txt")));
		return behaviorProperties;
	}

	private Properties createWorkloadIntensity(int numberOfUsers) throws IOException {
		Properties properties = new Properties();
		properties.put("workloadIntensity.type", "constant");
		properties.put("wl.type.value", Integer.toString(numberOfUsers));

		properties.store(Files.newOutputStream(workingDir.resolve("workloadIntensity.properties"), StandardOpenOption.CREATE), null);
		return properties;
	}

	private WorkloadModel generateWessbasModel(Properties workloadIntensityProperties, Properties behaviorModelsProperties) throws FileNotFoundException, SecurityException, GeneratorException {
		M4jdslModelGenerator generator = new M4jdslModelGenerator();
		final String sessionDatFilePath = workingDir.resolve("sessions.dat").toString();

		return generator.generateWorkloadModel(workloadIntensityProperties, behaviorModelsProperties, null, sessionDatFilePath, false);
	}

}