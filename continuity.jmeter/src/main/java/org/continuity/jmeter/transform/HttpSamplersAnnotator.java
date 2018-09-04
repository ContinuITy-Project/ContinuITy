package org.continuity.jmeter.transform;

import java.util.List;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.JsonPathExtraction;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.annotation.ValueExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

/**
 * @author Henning Schulz
 *
 */
public class HttpSamplersAnnotator extends AbstractSamplerAnnotator {

	public HttpSamplersAnnotator(Application system, ApplicationAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSamplerBySystemAnnotation(HTTPSamplerProxy sampler, ApplicationAnnotation annotation, HashTree samplerTree) {
		overrideHttpInterfaceProperties(sampler, annotation.getOverrides());
	}

	@Override
	protected void annotateHttpSamplerByInterfaceAnnotation(HTTPSamplerProxy sampler, EndpointAnnotation annotation, HashTree samplerTree) {
		overrideHttpInterfaceProperties(sampler, annotation.getOverrides());

		addRegExExtractions(annotation, samplerTree.getTree(sampler));
		new HttpArgumentsAnnotator(getSystem(), getAnnotation(), annotation).annotateArguments(sampler);
	}

	private void addRegExExtractions(EndpointAnnotation annotation, HashTree samplerTree) {
		IdpaByClassSearcher<ExtractedInput> searcher = new IdpaByClassSearcher<>(ExtractedInput.class, e -> onExtractionFound(annotation, samplerTree, e));
		searcher.visit(getAnnotation());
	}

	private void onExtractionFound(EndpointAnnotation annotation, HashTree samplerTree, IdpaElement element) {
		ExtractedInput input = (ExtractedInput) element;

		for (ValueExtraction extraction : input.getExtractions()) {
			if (extraction.getFrom().getId().equals(annotation.getAnnotatedEndpoint().getId())) {
				String id = input.getId();

				if (extraction instanceof RegExExtraction) {
					samplerTree.add(new ListedHashTree(createRegexExtractor((RegExExtraction) extraction, id)));
				} else if (extraction instanceof JsonPathExtraction) {
					samplerTree.add(new ListedHashTree(createJsonPostProcessor((JsonPathExtraction) extraction, id)));
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

	private <T extends PropertyOverrideKey.Any> void overrideHttpInterfaceProperties(HTTPSamplerProxy sampler, List<PropertyOverride<T>> overrides) {
		for (PropertyOverride<?> override : overrides) {
			if (override.getKey().isInScope(PropertyOverrideKey.HttpEndpoint.class)) {
				switch ((PropertyOverrideKey.HttpEndpoint) override.getKey()) {
				case DOMAIN:
					sampler.setDomain(override.getValue());
					break;
				case ENCODING:
					sampler.setContentEncoding(override.getValue());
					break;
				case PORT:
					sampler.setPort(Integer.parseInt(override.getValue()));
					break;
				case PROTOCOL:
					sampler.setProtocol(override.getValue());
					break;
				default:
					// do nothing
					break;
				}
			}
		}
	}

}
