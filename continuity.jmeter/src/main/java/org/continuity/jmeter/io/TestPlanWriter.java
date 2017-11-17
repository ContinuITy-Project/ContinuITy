package org.continuity.jmeter.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.apache.jorphan.collections.ListedHashTree;

import net.sf.markov4jmeter.testplangenerator.JMeterEngineGateway;
import net.sf.markov4jmeter.testplangenerator.TestPlanGenerator;
import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * @author Henning Schulz
 *
 */
public class TestPlanWriter {

	/**
	 * Initializes JMeter.
	 *
	 * @param jmeterHome
	 *            The path to the root JMeter configuration folder.
	 */
	public TestPlanWriter(String jmeterHome) {
		JMeterEngineGateway.getInstance().initJMeter(jmeterHome, jmeterHome + "/jmeter.properties", Locale.ENGLISH);
	}

	private final GeneratorAdapter generatorAdapter = new GeneratorAdapter();

	private final CSVHandler csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

	public Path write(ListedHashTree testPlanTree, Map<String, String[][]> behavior, Path outputDir) {
		Path testplanPath = outputDir.resolve("testplan.jmx");
		boolean succeeded = generatorAdapter.writeOutput(testPlanTree, testplanPath.toString());

		for (Map.Entry<String, String[][]> entry : behavior.entrySet()) {
			try {
				csvHandler.writeValues(entry.getKey(), entry.getValue());
			} catch (SecurityException | NullPointerException | IOException e) {
				e.printStackTrace();
				succeeded = false;
				break;
			}
		}

		if (!succeeded) {
			return null;
		}

		return testplanPath;
	}

	private static class GeneratorAdapter extends TestPlanGenerator {

		@Override
		public boolean writeOutput(ListedHashTree testPlanTree, String outputFilename) {
			return super.writeOutput(testPlanTree, outputFilename);
		}

	}

}
