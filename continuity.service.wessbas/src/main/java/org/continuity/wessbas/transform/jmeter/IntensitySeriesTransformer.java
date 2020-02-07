package org.continuity.wessbas.transform.jmeter;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jmeter.config.Arguments;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.wessbas.managers.WessbasPipelineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.voorn.markov4jmeter.control.MarkovController;

/**
 *
 * @author Henning Schulz
 *
 */
public class IntensitySeriesTransformer {

	public static final String PREFIX_INTENSITY = "continuity.intensity.";

	private static final String GROOVY_TEMPLATE = "${__groovy(vars.get(\"continuity.intensity.%s\").tokenize(\"\\,\")[Math.min(%d\\, (System.currentTimeMillis() - Long.parseLong(props.get(\"TEST.START.MS\"))) / %d as Integer)])}";

	private static final Logger LOGGER = LoggerFactory.getLogger(IntensitySeriesTransformer.class);

	public void transform(ListedHashTree testPlan, Map<String, String> intensitiesPerGroup, Integer resolution) {
		if ((intensitiesPerGroup != null) && (resolution != null)) {
			LOGGER.info("Setting the following workload intensity series with resolution {} ms: {}", resolution, intensitiesPerGroup);

			setIntensityVariables(testPlan, intensitiesPerGroup);
			setSessionArrivalFunctions(testPlan, intensitiesPerGroup, resolution);
		}
	}

	private void setIntensityVariables(ListedHashTree testPlan, Map<String, String> intensitiesPerGroup) {
		SearchByClass<Arguments> search = new SearchByClass<>(Arguments.class);
		testPlan.traverse(search);

		Collection<Arguments> searchResult = search.getSearchResults();

		if (searchResult.size() != 1) {
			throw new RuntimeException("Number of Arguments in test plan was " + searchResult.size() + "!");
		}

		// Only one iteration!
		for (Arguments args : search.getSearchResults()) {
			for (Entry<String, String> entry : intensitiesPerGroup.entrySet()) {
				args.addArgument(PREFIX_INTENSITY + entry.getKey(), entry.getValue());
			}
		}
	}

	private void setSessionArrivalFunctions(ListedHashTree testPlan, Map<String, String> intensitiesPerGroup, int resolution) {
		SearchByClass<MarkovController> search = new SearchByClass<>(MarkovController.class);
		testPlan.traverse(search);

		for (MarkovController controller : search.getSearchResults()) {
			controller.setArrivalCtrlEnabled(true);
			controller.setArrivalCtrlLoggingEnabled(false);

			String group = extractGroupName(controller.getName());
			String intensitySeries = intensitiesPerGroup.get(group);

			if (intensitySeries == null) {
				throw new RuntimeException("There is no intensity series for group " + group + "!");
			}

			controller.setArrivalCtrlNumSessions(formatGroovyScript(group, intensitySeries, resolution));
		}
	}

	private String extractGroupName(String controllerName) {
		if (controllerName.startsWith(WessbasPipelineManager.PREFIX_BEHAVIOR_MODEL)) {
			return controllerName.substring(WessbasPipelineManager.PREFIX_BEHAVIOR_MODEL.length());
		} else {
			return ForecastIntensityRecord.KEY_TOTAL;
		}
	}

	private String formatGroovyScript(String group, String intensitySeries, int resolution) {
		int maxIndex = intensitySeries.split(",").length - 1;
		return String.format(GROOVY_TEMPLATE, group, maxIndex, resolution);
	}

}
