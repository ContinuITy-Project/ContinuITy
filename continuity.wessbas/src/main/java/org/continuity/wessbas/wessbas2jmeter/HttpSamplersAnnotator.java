package org.continuity.wessbas.wessbas2jmeter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.workload.dsl.ContinuityModelElement;
import org.continuity.workload.dsl.annotation.ExtractedInput;
import org.continuity.workload.dsl.annotation.InterfaceAnnotation;
import org.continuity.workload.dsl.annotation.PropertyOverride;
import org.continuity.workload.dsl.annotation.PropertyOverrideKey;
import org.continuity.workload.dsl.annotation.RegExExtraction;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.HttpInterface;
import org.continuity.workload.dsl.system.ServiceInterface;
import org.continuity.workload.dsl.system.TargetSystem;
import org.continuity.workload.dsl.visitor.ContinuityByClassSearcher;

/**
 * @author Henning Schulz
 *
 */
public class HttpSamplersAnnotator {

	private static final Pattern REQUEST_PATTERN = Pattern.compile("R\\d+\\s\\((.*)\\)");

	private final TargetSystem system;

	private final SystemAnnotation systemAnnotation;

	public HttpSamplersAnnotator(TargetSystem system, SystemAnnotation systemAnnotation) {
		this.system = system;
		this.systemAnnotation = systemAnnotation;
	}

	public void annotateSamplers(ListedHashTree testPlan) {
		ContinuityByClassSearcher<InterfaceAnnotation> searcher = new ContinuityByClassSearcher<>(InterfaceAnnotation.class, e -> annotateInterface(e, testPlan));
		searcher.visit(systemAnnotation);
	}

	private void annotateInterface(ContinuityModelElement element, ListedHashTree testPlan) {
		InterfaceAnnotation annotation = (InterfaceAnnotation) element;

		ServiceInterface<?> interf = annotation.getAnnotatedInterface().resolve(system);

		if (interf instanceof HttpInterface) {
			annotateHttpInterface(annotation, testPlan);
		} else {
			throw new RuntimeException("Annotation of " + interf.getClass().getSimpleName() + " in JMeter is not yet implemented!");
		}
	}

	private void annotateHttpInterface(InterfaceAnnotation annotation, ListedHashTree testPlan) {
		SearchByClass<HTTPSamplerProxy> search = new SearchByClass<>(HTTPSamplerProxy.class);
		testPlan.traverse(search);

		for (HTTPSamplerProxy sampler : search.getSearchResults()) {
			overrideHttpInterfaceProperties(sampler, systemAnnotation.getOverrides());

			if (areEqualRequests(annotation.getAnnotatedInterface().getId(), sampler.getName())) {
				overrideHttpInterfaceProperties(sampler, annotation.getOverrides());

				addRegExExtractions(annotation, search.getSubTree(sampler).getTree(sampler));
				new HttpArgumentsAnnotator(system, systemAnnotation, annotation).annotateArguments(sampler);
			}
		}
	}

	private void addRegExExtractions(InterfaceAnnotation annotation, HashTree samplerTree) {
		ContinuityByClassSearcher<ExtractedInput> searcher = new ContinuityByClassSearcher<>(ExtractedInput.class, e -> onExtractionFound(annotation, samplerTree, e));
		searcher.visit(systemAnnotation);
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

	private boolean areEqualRequests(String interfaceId, String jmeterSamplerName) {
		Matcher matcher = REQUEST_PATTERN.matcher(jmeterSamplerName);
		matcher.find();
		boolean idMatchesName = matcher.group(1).equals(interfaceId);
		boolean equal = jmeterSamplerName.equals(interfaceId);
		return idMatchesName || equal;
	}

}
