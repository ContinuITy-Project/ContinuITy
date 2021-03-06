package org.continuity.jmeter.transform;

import java.util.Collection;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;

/**
 * This class deletes all user defined variables, in order to find
 *
 * @author Tobias Angerstein
 *
 */
public class UserDefinedDefaultVariablesCleanerAnnotator {

	public void cleanVariables(ListedHashTree testPlan) {
		SearchByClass<Arguments> search = new SearchByClass<>(Arguments.class);
		testPlan.traverse(search);

		Collection<Arguments> searchResult = search.getSearchResults();

		if (searchResult.size() != 1) {
			throw new RuntimeException("Number of Arguments in test plan was " + searchResult.size() + "!");
		}

		// Only one iteration!
		for (Arguments args : search.getSearchResults()) {
			PropertyIterator it = args.getArguments().iterator();

			while (it.hasNext()) {
				JMeterProperty prop = it.next();

				if (!prop.getName().startsWith("continuity")) {
					it.remove();
				}
			}
		}
	}
}
