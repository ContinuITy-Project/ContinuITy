package org.continuity.commons.jmeter;

import java.nio.file.Path;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;

import net.voorn.markov4jmeter.control.BehaviorMixEntry;
import net.voorn.markov4jmeter.control.MarkovController;

/**
 * Utility class to correct or change properties in a JMeter test plan.
 *
 * @author Henning Schulz
 *
 */
public class JMeterPropertiesCorrector {

	/**
	 * Sets the paths of the behavior model files in the specified JMeter test plan to the specified
	 * dir.
	 *
	 * @param testPlan
	 *            Test plan with wrong behavior model paths.
	 * @param dir
	 *            The root dir where the behavior models are stored.
	 */
	public void correctPaths(ListedHashTree testPlan, Path dir) {
		SearchByClass<MarkovController> search = new SearchByClass<>(MarkovController.class);
		testPlan.traverse(search);

		// Should be only one
		for (MarkovController controller : search.getSearchResults()) {

			JMeterProperty property = controller.getBehaviorMix().getProperty("UserBehaviorMix.behaviorEntries");

			if (!(property instanceof CollectionProperty)) {
				throw new IllegalArgumentException("Found a Markov Controller but it holds a property different from CollectionProperty as UserBehaviorMix.behaviorEntries");
			}

			CollectionProperty collProp = (CollectionProperty) property;
			PropertyIterator it = collProp.iterator();

			while (it.hasNext()) {
				Object propertyObject = it.next().getObjectValue();

				if (!(propertyObject instanceof BehaviorMixEntry)) {
					throw new IllegalArgumentException("Expected UserBehaviorMix.behaviorEntries to hold BehaviorMixEntry, but found " + propertyObject.getClass());
				}

				BehaviorMixEntry entry = (BehaviorMixEntry) propertyObject;

				Path fullPath = dir.resolve(entry.getBName() + ".csv");
				entry.setFilename(fullPath.toString());
			}
		}
	}

	/**
	 * Sets a CSV file for writing the results and configures the stored results.
	 *
	 * @param testPlan
	 *            The test plan that should write its results to the file.
	 * @param resultCsvPath
	 *            The path to the CSV.
	 */
	public void configureResultFile(ListedHashTree testPlan, Path resultCsvPath) {
		SearchByClass<ResultCollector> search = new SearchByClass<>(ResultCollector.class);
		testPlan.traverse(search);

		// Should be only one
		for (ResultCollector collector : search.getSearchResults()) {
			collector.setFilename(resultCsvPath.toAbsolutePath().toString());

			configureResultCollectorProperties(collector);
		}
	}

	private void configureResultCollectorProperties(ResultCollector collector) {
		JMeterProperty property = collector.getProperty("saveConfig");

		if (!(property instanceof ObjectProperty)) {
			throw new IllegalArgumentException("Found a Result Collector but it holds a property different from ObjectProperty as saveConfig");
		}

		Object object = ((ObjectProperty) property).getObjectValue();

		if (!(object instanceof SampleSaveConfiguration)) {
			throw new IllegalArgumentException("Expected saveConfig to hold a SampleSaveConfiguration, but found a " + object.getClass());
		}

		SampleSaveConfiguration config = (SampleSaveConfiguration) object;

		config.setAsXml(false);
		config.setTime(true);
		config.setMessage(true);
		config.setSuccess(true);
		config.setThreadCounts(true);
		config.setLatency(true);
		config.setSampleCount(true);
		config.setLabel(true);
		config.setThreadName(true);
		config.setTimestamp(true);
		config.setCode(true);
		config.setDataType(true);
		config.setBytes(true);
	}

	/**
	 * Does some corrections for running the test in headless mode. E.g., removes<br>
	 * {@code <stringProp name="CookieManager.implementation">HC3CookieHandler</stringProp>}<br>
	 * from the CookieManager.
	 *
	 * @param testPlan
	 *            The test plan to be corrected.
	 */
	public void prepareForHeadlessExecution(ListedHashTree testPlan) {
		SearchByClass<CookieManager> search = new SearchByClass<>(CookieManager.class);
		testPlan.traverse(search);

		for (CookieManager cookieManager : search.getSearchResults()) {
			cookieManager.removeProperty("CookieManager.implementation");
		}
	}

	/**
	 * Sets the number of users, the duration and the ramp up time.
	 *
	 * @param numUsers
	 *            Number of users to be executed in parallel.
	 * @param durationSeconds
	 *            The duration od the test in seconds.
	 * @param rampupSeconds
	 *            The ramp up time in seconds.
	 */
	public void setRuntimeProperties(ListedHashTree testPlan, int numUsers, long durationSeconds, int rampupSeconds) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		for (ThreadGroup group : search.getSearchResults()) {
			group.setNumThreads(numUsers);
			group.setRampUp(rampupSeconds);
			group.setScheduler(true);
			group.setDuration(durationSeconds);

			Controller mainController = group.getSamplerController();

			if (mainController instanceof LoopController) {
				// Sets Loop Count: Forever [CHECK]
				((LoopController) mainController).setLoops(-1);
			}
		}
	}
	
	/**
	 * Sets the duration and the ramp up time.
	 *
	 * @param durationSeconds
	 *            The duration of the test in seconds.
	 * @param rampupSeconds
	 *            The ramp up time in seconds.
	 */
	public void setRuntimeProperties(ListedHashTree testPlan, long durationSeconds, int rampupSeconds) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		for (ThreadGroup group : search.getSearchResults()) {
			group.setRampUp(rampupSeconds);
			group.setScheduler(true);
			group.setDuration(durationSeconds);

			Controller mainController = group.getSamplerController();

			if (mainController instanceof LoopController) {
				// Sets Loop Count: Forever [CHECK]
				((LoopController) mainController).setLoops(-1);
			}
		}
	}

}
