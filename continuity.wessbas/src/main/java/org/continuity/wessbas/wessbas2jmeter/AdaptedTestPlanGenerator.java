package org.continuity.wessbas.wessbas2jmeter;

import org.apache.jorphan.collections.ListedHashTree;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.testplangenerator.TestPlanGenerator;
import net.sf.markov4jmeter.testplangenerator.transformation.AbstractTestPlanTransformer;
import net.sf.markov4jmeter.testplangenerator.transformation.TransformationException;
import net.sf.markov4jmeter.testplangenerator.transformation.filters.AbstractFilter;

/**
 * @author Henning Schulz
 *
 */
public class AdaptedTestPlanGenerator extends TestPlanGenerator {

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
