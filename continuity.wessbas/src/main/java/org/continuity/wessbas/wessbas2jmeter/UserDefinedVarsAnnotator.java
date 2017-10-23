package org.continuity.wessbas.wessbas2jmeter;

import java.util.Collection;

import org.apache.jmeter.config.Arguments;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.workload.dsl.annotation.DirectDataInput;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.TargetSystem;

/**
 * @author Henning Schulz
 *
 */
public class UserDefinedVarsAnnotator {

	private final TargetSystem system;

	private final SystemAnnotation systemAnnotation;

	public UserDefinedVarsAnnotator(TargetSystem system, SystemAnnotation systemAnnotation) {
		this.system = system;
		this.systemAnnotation = systemAnnotation;
	}

	public void annotateVariables(ListedHashTree testPlan) {
		SearchByClass<Arguments> search = new SearchByClass<>(Arguments.class);
		testPlan.traverse(search);

		Collection<Arguments> searchResult = search.getSearchResults();

		if (searchResult.size() != 1) {
			throw new RuntimeException("Number of Arguments in test plan was " + searchResult.size() + "!");
		}

		// Only one iteration!
		for (Arguments args : search.getSearchResults()) {
			args.getArguments().clear();
			addDirectDataInputs(args);
		}

		System.out.println();
	}

	private void addDirectDataInputs(final Arguments args) {
		systemAnnotation.getInputs().stream().filter(input -> input instanceof DirectDataInput).map(input -> (DirectDataInput) input).forEach(input -> {
			if (input.getData().size() > 1) {
				StringBuilder builder = new StringBuilder();
				for (String dat : input.getData()) {
					builder.append(dat);
					builder.append(";");
				}

				args.addArgument(input.getId(), builder.toString());
			}
		});
	}

}
