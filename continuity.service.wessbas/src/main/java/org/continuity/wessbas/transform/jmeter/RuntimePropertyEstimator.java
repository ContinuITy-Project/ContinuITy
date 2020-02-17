package org.continuity.wessbas.transform.jmeter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
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

	/**
	 * Adjusts the runtime properties and returns them.
	 * 
	 * @param testPlan
	 * @param intensitiesPerGroup
	 * @param resolution
	 * @return A pair of (rampup, duration) in seconds.
	 */
	public Pair<Integer, Long> adjustAndReturn(ListedHashTree testPlan, Map<String, String> intensitiesPerGroup, Integer resolution) {
		List<Integer> intensities = (intensitiesPerGroup == null) || intensitiesPerGroup.isEmpty() ? Collections.singletonList(getNumUsers(testPlan)) : totalIntensities(intensitiesPerGroup);

		long duration = estimateDuration(intensities, resolution);
		int rampup = estimateRampup(intensities, duration);

		jmeterPropertiesCorrector.setDuration(testPlan, duration + rampup);
		jmeterPropertiesCorrector.setRampup(testPlan, rampup);

		return Pair.of(rampup, duration);
	}

	private List<Integer> totalIntensities(Map<String, String> intensitiesPerGroup) {
		List<List<Integer>> intensitySeries = intensitiesPerGroup.values().stream().map(groupInt -> Arrays.stream(groupInt.split(",")).map(Integer::parseInt).collect(Collectors.toList()))
				.collect(Collectors.toList());

		return IntStream.range(0, intensitySeries.stream().mapToInt(List::size).min().orElse(0)).mapToObj(i -> intensitySeries.stream().mapToInt(l -> l.get(i)).sum()).collect(Collectors.toList());
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

		int numUsers = 0;

		for (ThreadGroup group : search.getSearchResults()) {
			numUsers += group.getNumThreads();
		}

		return numUsers == 0 ? 1 : numUsers;
	}

}
