package org.continuity.wessbas.transform.jmeter;

import java.io.IOException;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;

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

	/**
	 *
	 */
	public WessbasToJmeterConverter(String configurationPath, String outputPath, boolean writeToFile) {
		this.outputPath = outputPath;
		this.writeToFile = writeToFile;
		this.generator = new AdaptedTestPlanGenerator();
		this.generator.init(configurationPath + "/generator.default.properties", configurationPath + "/testplan.default.properties");
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
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @return A pack containing an executable load test corresponding to the load represented by
	 *         the workload model and the behaviors.
	 */
	public JMeterTestPlanBundle convertToLoadTest(WorkloadModel workloadModel) {
		fixBehaviorModelFilenames(workloadModel);

		CSVBufferingHandler csvHandler = new CSVBufferingHandler();
		SimpleTestPlanTransformer testPlanTransformer = new SimpleTestPlanTransformer(csvHandler, outputPath);
		AbstractFilter[] filters = { new HeaderDefaultsFilter() };

		ListedHashTree testPlan;

		try {
			testPlan = generator.generate(workloadModel, testPlanTransformer, filters);
		} catch (TransformationException e) {
			throw new RuntimeException("Error during JMeter Test Plan generation!", e);
		}

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

	private void fixBehaviorModelFilenames(WorkloadModel workloadModel) {
		for (BehaviorModel behaviorModel : workloadModel.getBehaviorModels()) {
			String filename = behaviorModel.getName() + ".csv";
			behaviorModel.setFilename(filename);
		}
	}

}
