package org.continuity.wessbas.transform.jmeter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.commons.jmeter.JMeterPropertiesCorrector;

/**
 *
 * @author Henning Schulz
 *
 */
public class RuntimePropertyEstimator {

	private static final int DEFAULT_DURATION = 3600;

	private JMeterPropertiesCorrector jmeterPropertiesCorrector = new JMeterPropertiesCorrector();

	public void adjust(ListedHashTree testPlan, String intensitySeries, Integer resolution) {
		List<Integer> intensities = intensitySeries == null ? Collections.singletonList(getNumUsers(testPlan))
				: Arrays.stream(intensitySeries.split(",")).map(Integer::parseInt).collect(Collectors.toList());

		long duration = estimateDuration(intensities, resolution);
		int rampup = estimateRampup(intensities, duration);

		jmeterPropertiesCorrector.setDuration(testPlan, duration);
		jmeterPropertiesCorrector.setRampup(testPlan, rampup);
	}

	private long estimateDuration(List<Integer> intensities, Integer resolution) {
		if ((intensities.size() <= 1) || (resolution == null)) {
			return DEFAULT_DURATION;
		} else {
			return intensities.size() * (resolution / 1000);
		}
	}

	private int estimateRampup(List<Integer> intensities, long duration) {
		return (int) Math.min(intensities.get(0), duration);
	}

	private int getNumUsers(ListedHashTree testPlan) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		int numUsers = 1;

		for (ThreadGroup group : search.getSearchResults()) {
			int numThreads = group.getNumThreads();

			if (numThreads > numUsers) {
				numUsers = numThreads;
			}
		}

		return numUsers;
	}

}
