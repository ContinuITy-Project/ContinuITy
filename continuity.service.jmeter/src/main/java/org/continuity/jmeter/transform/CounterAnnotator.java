package org.continuity.jmeter.transform;

import java.util.Collection;

import org.apache.jmeter.modifiers.CounterConfig;
import org.apache.jmeter.modifiers.gui.CounterConfigGui;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.idpa.annotation.CounterInput;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

import net.voorn.markov4jmeter.control.MarkovController;

/**
 * Adds counters to a JMeter test plan based on an annotation.
 *
 * @author Henning Schulz
 *
 */
public class CounterAnnotator {

	private final ApplicationAnnotation systemAnnotation;

	public CounterAnnotator(ApplicationAnnotation systemAnnotation) {
		this.systemAnnotation = systemAnnotation;
	}

	/**
	 * Adds the counters.
	 *
	 * @param testPlan
	 *            JMeter test plan to add the counters to.
	 */
	public void addCounters(ListedHashTree testPlan) {
		SearchByClass<MarkovController> search = new SearchByClass<>(MarkovController.class);
		testPlan.traverse(search);


		Collection<MarkovController> searchResult = search.getSearchResults();

		for (MarkovController markovController : searchResult) {
			new IdpaByClassSearcher<>(CounterInput.class, input -> addCounterToThreadGroup(input, search.getSubTree(markovController).getTree(markovController))).visit(systemAnnotation);
		}
	}

	private void addCounterToThreadGroup(CounterInput input, HashTree markovTree) {
		CounterConfigGui counterGui = new CounterConfigGui();
		CounterConfig counter = (CounterConfig) counterGui.createTestElement();

		counter.setName("Counter (" + input.getId() + ")");
		counter.setStart(input.getStart());
		counter.setIncrement(input.getIncrement());
		counter.setEnd(input.getMaximum());

		if (input.getFormat() != null) {
			counter.setFormat(input.getFormat());
		}

		counter.setVarName(input.getId());

		switch (input.getScope()) {
		case GLOBAL:
			counter.setIsPerUser(false);
			counter.setResetOnThreadGroupIteration(false);
			break;
		case USER:
			counter.setIsPerUser(true);
			counter.setResetOnThreadGroupIteration(false);
			break;
		case USER_ITERATION:
			counter.setIsPerUser(true);
			counter.setResetOnThreadGroupIteration(true);
			break;
		default:
			break;
		}

		markovTree.add(new ListedHashTree(counter));
	}

}
