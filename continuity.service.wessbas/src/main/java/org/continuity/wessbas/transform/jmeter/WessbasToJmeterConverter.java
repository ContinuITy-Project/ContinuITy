package org.continuity.wessbas.transform.jmeter;

import java.io.IOException;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.wessbas.entities.WessbasBundle;

import m4jdsl.BehaviorModel;
import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.testplangenerator.transformation.SimpleTestPlanTransformer;
import net.sf.markov4jmeter.testplangenerator.transformation.TransformationException;
import net.sf.markov4jmeter.testplangenerator.transformation.filters.AbstractFilter;
import net.sf.markov4jmeter.testplangenerator.transformation.filters.HeaderDefaultsFilter;

/**
 * @author Henning Schulz
 *
 */
public class WessbasToJmeterConverter {

	private final String outputPath;

	private final boolean writeToFile;

	private final AdaptedTestPlanGenerator generator;

	private final IntensitySeriesTransformer intensityTransformer;

	private final RuntimePropertyEstimator runtimeEstimator;

	/**
	 *
	 */
	public WessbasToJmeterConverter(String configurationPath, String outputPath, boolean writeToFile) {
		this.outputPath = outputPath;
		this.writeToFile = writeToFile;
		this.generator = new AdaptedTestPlanGenerator();
		this.generator.init(configurationPath + "/generator.default.properties", configurationPath + "/testplan.default.properties");
		this.intensityTransformer = new IntensitySeriesTransformer();
		this.runtimeEstimator = new RuntimePropertyEstimator();
	}

	/**
	 *
	 */
	public WessbasToJmeterConverter(String configurationPath, String outputPath) {
		this(configurationPath, outputPath, outputPath != null);
	}

	/**
	 * Creates a new instance not writing to a file.
	 */
	public WessbasToJmeterConverter(String configurationPath) {
		this(configurationPath, null, false);
	}

	/**
	 * Converts the passed WESSBAS bundle to an executable load test.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @return A pack containing an executable load test corresponding to the load represented by
	 *         the workload model and the behaviors.
	 */
	public JMeterTestPlanBundle convertToLoadTest(WessbasBundle workloadModel) {
		fixBehaviorModelFilenames(workloadModel.getWorkloadModel());

		CSVBufferingHandler csvHandler = new CSVBufferingHandler();
		SimpleTestPlanTransformer testPlanTransformer = new SimpleTestPlanTransformer(csvHandler, outputPath);
		AbstractFilter[] filters = { new HeaderDefaultsFilter() };

		ListedHashTree testPlan;

		try {
			testPlan = generator.generate(workloadModel.getWorkloadModel(), testPlanTransformer, filters);
		} catch (TransformationException e) {
			throw new RuntimeException("Error during JMeter Test Plan generation!", e);
		}

		intensityTransformer.transform(testPlan, workloadModel.getIntensities(), workloadModel.getIntensityResolution());
		runtimeEstimator.adjust(testPlan, workloadModel.getIntensities(), workloadModel.getIntensityResolution());

		if (writeToFile) {
			generator.writeToFile(testPlan, outputPath + "/testplan.jmx");
			try {
				csvHandler.writeToDisk(outputPath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return new JMeterTestPlanBundle(testPlan, csvHandler.getBuffer());
	}

	/**
	 * Converts the passed workload model to an executable load test.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @return A pack containing an executable load test corresponding to the load represented by
	 *         the workload model and the behaviors.
	 */
	public JMeterTestPlanBundle convertToLoadTest(WorkloadModel workloadModel) {
		return convertToLoadTest(new WessbasBundle(null, workloadModel));
	}

	private void fixBehaviorModelFilenames(WorkloadModel workloadModel) {
		for (BehaviorModel behaviorModel : workloadModel.getBehaviorModels()) {
			String filename = behaviorModel.getName() + ".csv";
			behaviorModel.setFilename(filename);
		}
	}

}
