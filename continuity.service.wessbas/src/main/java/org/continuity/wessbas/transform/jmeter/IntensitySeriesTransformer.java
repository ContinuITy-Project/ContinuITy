package org.continuity.wessbas.transform.jmeter;

import java.util.Collection;

import org.apache.jmeter.config.Arguments;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.voorn.markov4jmeter.control.MarkovController;

/**
 *
 * @author Henning Schulz
 *
 */
public class IntensitySeriesTransformer {

	public static final String KEY_INTENSITY = "continuity.intensity";

	private static final String GROOVY_TEMPLATE = "${__groovy(vars.get(\"continuity.intensity\").tokenize(\"\\,\")[Math.min(%d\\, (System.currentTimeMillis() - Long.parseLong(props.get(\"TEST.START.MS\"))) / %d as Integer)])}";

	private static final Logger LOGGER = LoggerFactory.getLogger(IntensitySeriesTransformer.class);

	public void transform(ListedHashTree testPlan, String intensitySeries, Integer resolution) {
		if ((intensitySeries != null) && (resolution != null)) {
			LOGGER.info("Setting the following workload intensity series with resolution {} ms: {}", resolution, intensitySeries);

			setIntensityVariable(testPlan, intensitySeries);
			setSessionArrivalFunction(testPlan, intensitySeries, resolution);
		}
	}

	private void setIntensityVariable(ListedHashTree testPlan, String intensitySeries) {
		SearchByClass<Arguments> search = new SearchByClass<>(Arguments.class);
		testPlan.traverse(search);

		Collection<Arguments> searchResult = search.getSearchResults();

		if (searchResult.size() != 1) {
			throw new RuntimeException("Number of Arguments in test plan was " + searchResult.size() + "!");
		}

		// Only one iteration!
		for (Arguments args : search.getSearchResults()) {
			args.addArgument(KEY_INTENSITY, intensitySeries);
		}
	}

	private void setSessionArrivalFunction(ListedHashTree testPlan, String intensitySeries, int resolution) {
		SearchByClass<MarkovController> search = new SearchByClass<>(MarkovController.class);
		testPlan.traverse(search);

		Collection<MarkovController> searchResult = search.getSearchResults();

		if (searchResult.size() != 1) {
			throw new RuntimeException("Number of MarkovControllers in test plan was " + searchResult.size() + "!");
		}

		for (MarkovController controller : search.getSearchResults()) {
			controller.setArrivalCtrlEnabled(true);
			controller.setArrivalCtrlLoggingEnabled(false);
			controller.setArrivalCtrlNumSessions(formatGroovyScript(intensitySeries, resolution));
		}
	}

	private String formatGroovyScript(String intensitySeries, int resolution) {
		int maxIndex = intensitySeries.split(",").length - 1;
		return String.format(GROOVY_TEMPLATE, maxIndex, resolution);
	}

}
