package org.continuity.commons.utils;

import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;

/**
 * Provides utility functionality for JMeter test plans.
 *
 * @author Henning Schulz
 *
 */
public class JMeterUtils {

	private JMeterUtils() {
	}

	/**
	 * Extracts the configured duration of the test plan.
	 *
	 * @param testPlan
	 *            The test plan.
	 * @return The duration.
	 */
	public static long getDuration(ListedHashTree testPlan) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		for (ThreadGroup group : search.getSearchResults()) {
			return group.getDuration();
		}

		return -1;
	}

}
