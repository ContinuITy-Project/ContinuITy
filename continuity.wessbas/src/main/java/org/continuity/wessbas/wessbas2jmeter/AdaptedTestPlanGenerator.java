package org.continuity.wessbas.wessbas2jmeter;

import org.apache.jorphan.collections.ListedHashTree;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.testplangenerator.TestPlanGenerator;
import net.sf.markov4jmeter.testplangenerator.transformation.AbstractTestPlanTransformer;
import net.sf.markov4jmeter.testplangenerator.transformation.TransformationException;
import net.sf.markov4jmeter.testplangenerator.transformation.filters.AbstractFilter;

/**
 * Adapted {@link TestPlanGenerator} not directly writing the results to a file, but only on demand.
 *
 * @author Henning Schulz
 *
 */
public class AdaptedTestPlanGenerator extends TestPlanGenerator {

	/**
	 * Generates a Test Plan for the given workload model and writes the result into the specified
	 * file.
	 *
	 * @param workloadModel
	 *            workload model which provides the values for the Test Plan to be generated.
	 * @param testPlanTransformer
	 *            builder to be used for building a Test Plan of certain structure.
	 * @param filters
	 *            (optional) modification filters to be finally applied on the newly generated Test
	 *            Plan.
	 * @return The generated Test Plan, or <code>null</code> if any error occurs.
	 * @throws TransformationException
	 *             if any critical error in the transformation process occurs.
	 */
	public ListedHashTree generate(WorkloadModel workloadModel, AbstractTestPlanTransformer testPlanTransformer, AbstractFilter[] filters) throws TransformationException {
		return super.generate(workloadModel, testPlanTransformer, filters, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean writeOutput(ListedHashTree testPlanTree, String outputFilename) {
		// do nothing, to prevent writing during creation of the test plan
		return true;
	}

	public boolean writeToFile(ListedHashTree testPlanTree, String outputFilename) {
		return super.writeOutput(testPlanTree, outputFilename);
	}

}
