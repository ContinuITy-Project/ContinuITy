package org.continuity.jmeter.transform;

import java.util.stream.Collectors;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.extracted.ExtractedInput;
import org.continuity.idpa.annotation.extracted.JsonPathExtraction;
import org.continuity.idpa.annotation.extracted.RegExExtraction;
import org.continuity.idpa.annotation.extracted.ValueExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

public class ValueExtractorsAnnotator extends AbstractSamplerAnnotator {

	protected ValueExtractorsAnnotator(Application system, ApplicationAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSampler(HTTPSamplerProxy sampler, HttpEndpoint endpoint, EndpointAnnotation annotation, HashTree samplerTree) {
		IdpaByClassSearcher<ExtractedInput> searcher = new IdpaByClassSearcher<>(ExtractedInput.class, e -> onExtractionFound(annotation, samplerTree.getTree(sampler), e));
		searcher.visit(getAnnotation());
	}

	private void onExtractionFound(EndpointAnnotation annotation, HashTree samplerTree, IdpaElement element) {
		ExtractedInput input = (ExtractedInput) element;

		for (ValueExtraction extraction : input.getExtractions()) {
			if (extraction.getFrom().isEndpoint() && extraction.getFrom().getEndpoint().getId().equals(annotation.getAnnotatedEndpoint().getId())) {
				String id = input.getId();

				if (extraction instanceof RegExExtraction) {
					samplerTree.add(new ListedHashTree(createRegexExtractor((RegExExtraction) extraction, id)));
				} else if (extraction instanceof JsonPathExtraction) {
					samplerTree.add(new ListedHashTree(createJsonPostProcessor((JsonPathExtraction) extraction, id)));
				}
			}
		}

	}

	public void annotateInputs(ListedHashTree testPlan) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		for (ThreadGroup threadGroup : search.getSearchResults()) {
			addExtractionsToThreadGroup(search.getSubTree(threadGroup).getTree(threadGroup));

		}
	}

	private void addExtractionsToThreadGroup(HashTree threadGroupTree) {
		for (ExtractedInput extractedInput : getAnnotation().getInputs().stream().filter(ExtractedInput.class::isInstance).map(ExtractedInput.class::cast).collect(Collectors.toList())) {
			for (ValueExtraction extraction : extractedInput.getExtractions()) {
				if (extraction.getFrom().isInput()) {
					String id = extractedInput.getId();

					if (extraction instanceof RegExExtraction) {
						RegexExtractor extr = createRegexExtractor((RegExExtraction) extraction, id);
						extr.setScopeVariable(extraction.getFrom().getInput().getId());
						threadGroupTree.add(new ListedHashTree(extr));
					} else if (extraction instanceof JsonPathExtraction) {
						JSONPostProcessor proc = createJsonPostProcessor((JsonPathExtraction) extraction, id);
						proc.setScopeVariable(extraction.getFrom().getInput().getId());
						threadGroupTree.add(new ListedHashTree(proc));
					}
				}
			}
		}
	}

	private RegexExtractor createRegexExtractor(RegExExtraction extraction, String id) {
		RegexExtractorGui gui = new RegexExtractorGui();
		RegexExtractor extractor = (RegexExtractor) gui.createTestElement();

		extractor.setRefName(id);
		extractor.setRegex(extraction.getPattern());

		// JMeter uses $1$ for marking the first group
		extractor.setTemplate(extraction.getTemplate().replace("(", "$").replace(")", "$"));
		extractor.setMatchNumber(extraction.getMatchNumber());
		extractor.setDefaultValue(extraction.getFallbackValue());

		return extractor;
	}

	private JSONPostProcessor createJsonPostProcessor(JsonPathExtraction extraction, String id) {
		JSONPostProcessorGui gui = new JSONPostProcessorGui();
		JSONPostProcessor processor = (JSONPostProcessor) gui.createTestElement();

		processor.setRefNames(id);
		processor.setJsonPathExpressions(extraction.getJsonPath());
		processor.setMatchNumbers(Integer.toString(extraction.getMatchNumber()));
		processor.setDefaultValues(extraction.getFallbackValue());

		processor.setComputeConcatenation(false);

		return processor;
	}

}
