package org.continuity.jmeter.transform;

import java.util.List;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.ann.ExtractedInput;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.PropertyOverride;
import org.continuity.annotation.dsl.ann.PropertyOverrideKey;
import org.continuity.annotation.dsl.ann.RegExExtraction;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;

/**
 * @author Henning Schulz
 *
 */
public class HttpSamplersAnnotator extends AbstractSamplerAnnotator {

	public HttpSamplersAnnotator(SystemModel system, SystemAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSamplerBySystemAnnotation(HTTPSamplerProxy sampler, SystemAnnotation annotation, HashTree samplerTree) {
		overrideHttpInterfaceProperties(sampler, annotation.getOverrides());
	}

	@Override
	protected void annotateHttpSamplerByInterfaceAnnotation(HTTPSamplerProxy sampler, InterfaceAnnotation annotation, HashTree samplerTree) {
		overrideHttpInterfaceProperties(sampler, annotation.getOverrides());

		addRegExExtractions(annotation, samplerTree.getTree(sampler));
		new HttpArgumentsAnnotator(getSystem(), getAnnotation(), annotation).annotateArguments(sampler);
	}

	private void addRegExExtractions(InterfaceAnnotation annotation, HashTree samplerTree) {
		ContinuityByClassSearcher<ExtractedInput> searcher = new ContinuityByClassSearcher<>(ExtractedInput.class, e -> onExtractionFound(annotation, samplerTree, e));
		searcher.visit(getAnnotation());
	}

	private void onExtractionFound(InterfaceAnnotation annotation, HashTree samplerTree, ContinuityModelElement element) {
		ExtractedInput input = (ExtractedInput) element;

		for (RegExExtraction extraction : input.getExtractions()) {
			if (extraction.getFrom().getId().equals(annotation.getAnnotatedInterface().getId())) {
				RegexExtractorGui gui = new RegexExtractorGui();
				RegexExtractor extractor = (RegexExtractor) gui.createTestElement();

				extractor.setRefName(input.getId());
				extractor.setRegex(extraction.getPattern());

				// JMeter uses $1$ for marking the first group
				extractor.setTemplate(extraction.getTemplate().replace("(", "$").replace(")", "$"));
				extractor.setMatchNumber(extraction.getMatchNumber());
				extractor.setDefaultValue(extraction.getFallbackValue());

				samplerTree.add(new ListedHashTree(extractor));
			}
		}
	}

	private <T extends PropertyOverrideKey.Any> void overrideHttpInterfaceProperties(HTTPSamplerProxy sampler, List<PropertyOverride<T>> overrides) {
		for (PropertyOverride<?> override : overrides) {
			if (override.getKey().isInScope(PropertyOverrideKey.HttpInterface.class)) {
				switch ((PropertyOverrideKey.HttpInterface) override.getKey()) {
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
