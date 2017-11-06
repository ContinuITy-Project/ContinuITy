package org.continuity.wessbas.wessbas2jmeter;

import java.io.IOException;

import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.commons.exceptions.AnnotationNotSupportedException;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtension;
import org.continuity.workload.dsl.annotation.ext.AnnotationExtensionElement;
import org.continuity.workload.dsl.system.TargetSystem;

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

	private final AdaptedTestPlanGenerator generator;

	/**
	 *
	 */
	public WessbasToJmeterConverter(String outputPath) {
		this.outputPath = outputPath;
		this.generator = new AdaptedTestPlanGenerator();
		this.generator.init("configuration/generator.default.properties", "configuration/testplan.default.properties");
	}

	/**
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @param system
	 *            The system representation.
	 * @param annotation
	 *            The system annotation.
	 * @param extension
	 *            An extension of the annotation. May cause an
	 *            {@link AnnotationNotSupportedException}.
	 * @return An executable load test corresponding to the load represented by the workload model.
	 * @throws AnnotationNotSupportedException
	 *             if the passed {@link AnnotationExtension} cannot be converted to the load test.
	 */
	public ListedHashTree convertToWorkload(WorkloadModel workloadModel, TargetSystem system, SystemAnnotation annotation, AnnotationExtension extension) throws AnnotationNotSupportedException {
		if ((extension != null) && !extension.getElements().isEmpty()) {
			StringBuilder builder = new StringBuilder();
			builder.append("The following extensoins are not supported: ");

			for (AnnotationExtensionElement element : extension.getElements().values()) {
				builder.append(element.getId());
				builder.append(", ");
			}

			throw new AnnotationNotSupportedException(builder.substring(0, builder.length() - 2));
		}

		return convertToWorkload(workloadModel, system, annotation);
	}

	/**
	 * Converts the passed workload model and annotations to an executable load test. The annotation
	 * models are to be linked.
	 *
	 * @param workloadModel
	 *            The workload model.
	 * @param system
	 *            The system representation.
	 * @param annotation
	 *            The system annotation.
	 * @return An executable load test corresponding to the load represented by the workload model.
	 */
	public ListedHashTree convertToWorkload(WorkloadModel workloadModel, TargetSystem system, SystemAnnotation annotation) {
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

		JMeterAnnotator annotator = new JMeterAnnotator(testPlan, system);
		annotator.addAnnotations(annotation);
		generator.writeToFile(testPlan, outputPath + "/testplan.jmx");
		try {
			csvHandler.writeToDisk(outputPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return testPlan;
	}

	private void fixBehaviorModelFilenames(WorkloadModel workloadModel) {
		for (BehaviorModel behaviorModel : workloadModel.getBehaviorModels()) {
			String filename = behaviorModel.getName() + ".csv";
			behaviorModel.setFilename(filename);
		}
	}

}
