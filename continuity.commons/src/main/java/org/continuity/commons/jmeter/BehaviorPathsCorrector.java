package org.continuity.commons.jmeter;

import java.nio.file.Path;

import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;

import net.voorn.markov4jmeter.control.BehaviorMixEntry;
import net.voorn.markov4jmeter.control.MarkovController;

/**
 * Utility class to set the paths of the behavior model files in a JMeter test plan to the correct
 * path.
 *
 * @author Henning Schulz
 *
 */
public class BehaviorPathsCorrector {

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

}
