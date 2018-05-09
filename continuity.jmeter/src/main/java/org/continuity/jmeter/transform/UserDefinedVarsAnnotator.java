package org.continuity.jmeter.transform;

import java.util.Collection;

import org.apache.jmeter.config.Arguments;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.ApplicationAnnotation;

/**
 * @author Henning Schulz
 *
 */
public class UserDefinedVarsAnnotator {

	private final ApplicationAnnotation systemAnnotation;

	public UserDefinedVarsAnnotator(ApplicationAnnotation systemAnnotation) {
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
			addExtractedInputsInitialValues(args);
		}
	}

	private void addDirectDataInputs(final Arguments args) {
		systemAnnotation.getInputs().stream().filter(input -> input instanceof DirectListInput).map(input -> (DirectListInput) input).forEach(input -> {
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

	private void addExtractedInputsInitialValues(final Arguments args) {
		systemAnnotation.getInputs().stream().filter(input -> input instanceof ExtractedInput).map(input -> (ExtractedInput) input).forEach(input -> {
			if (input.getInitialValue() != null) {
				args.addArgument(input.getId(), input.getInitialValue());
			}
		});
	}

}
