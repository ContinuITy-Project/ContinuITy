package org.continuity.wessbas.wessbas2jmeter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.workload.dsl.annotation.ExtractedInput;
import org.continuity.workload.dsl.annotation.Input;
import org.continuity.workload.dsl.annotation.InterfaceAnnotation;
import org.continuity.workload.dsl.annotation.PropertyOverride;
import org.continuity.workload.dsl.annotation.PropertyOverrideKey;
import org.continuity.workload.dsl.annotation.RegExExtraction;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.TargetSystem;
import org.continuity.workload.dsl.visitor.ContinuityByClassSearcher;

import net.sf.markov4jmeter.testplangenerator.TestPlanElementFactory;

/**
 * @author Henning Schulz
 *
 */
public class JMeterAnnotator {

	private static final Pattern REQUEST_PATTERN = Pattern.compile("R\\d+\\s\\((.*)\\)");

	private final ListedHashTree testPlan;

	private final TargetSystem system;

	private final TestPlanElementFactory factory;

	public JMeterAnnotator(ListedHashTree testPlan, TargetSystem system, TestPlanElementFactory factory) {
		this.testPlan = testPlan;
		this.system = system;
		this.factory = factory;
	}

	public void addAnnotations(SystemAnnotation annotation) {
		addSystemOverrides(annotation.getOverrides());
		addInputs(annotation.getInputs());
		addHttpInterfaceAnnotations(annotation);
	}

	private void addSystemOverrides(List<PropertyOverride<PropertyOverrideKey.Any>> overrides) {
		// TODO: quick and dirty...

		for (PropertyOverride<PropertyOverrideKey.Any> ov : overrides) {
			if (ov.getKey() == PropertyOverrideKey.HttpInterface.DOMAIN) {
				SearchByClass<HTTPSamplerProxy> samplerSearch = new SearchByClass<>(HTTPSamplerProxy.class);
				testPlan.traverse(samplerSearch);

				for (HTTPSamplerProxy sampler : samplerSearch.getSearchResults()) {
					overrideHttpSampler(sampler, ov);
				}
			}
		}
	}

	private void addInputs(List<Input> inputs) {
		// TODO: quick and dirty...

		for (Input input : inputs) {
			if (input instanceof ExtractedInput) {
				for (RegExExtraction extraction : ((ExtractedInput) input).getExtractions()) {
					SearchByClass<HTTPSamplerProxy> samplerSearch = new SearchByClass<>(HTTPSamplerProxy.class);
					testPlan.traverse(samplerSearch);

					for (HTTPSamplerProxy sampler : samplerSearch.getSearchResults()) {
						Matcher matcher = REQUEST_PATTERN.matcher(sampler.getName());
						matcher.find();
						if (matcher.group(1).equals(extraction.getFrom().getId()) || sampler.getName().equals(extraction.getFrom().getId())) {
							// RegexExtractor extractor = factory.createRegexExtractor();
							RegexExtractorGui gui = new RegexExtractorGui();
							RegexExtractor extractor = (RegexExtractor) gui.createTestElement();

							extractor.setRefName(input.getId());
							extractor.setRegex(extraction.getPattern());
							extractor.setTemplate(extraction.getTemplate());
							extractor.setMatchNumber(extraction.getMatchNumber());
							extractor.setDefaultValue(extraction.getFallbackValue());

							samplerSearch.getSubTree(sampler).getTree(sampler).add(new ListedHashTree(extractor));
						}
					}
				}
			}
		}
	}

	private void addHttpInterfaceAnnotations(SystemAnnotation annotation) {
		ContinuityByClassSearcher<InterfaceAnnotation> searcher = new ContinuityByClassSearcher<>(InterfaceAnnotation.class, this::annotateHttpInterface);
		searcher.visit(annotation);
	}

	private boolean annotateHttpInterface(InterfaceAnnotation annotation) {
		SearchByClass<HTTPSamplerProxy> samplerSearch = new SearchByClass<>(HTTPSamplerProxy.class);
		testPlan.traverse(samplerSearch);

		for (HTTPSamplerProxy sampler : samplerSearch.getSearchResults()) {
			Matcher matcher = REQUEST_PATTERN.matcher(sampler.getName());
			matcher.find();
			if (matcher.group(1).equals(annotation.getAnnotatedInterface().getId()) || sampler.getName().equals(annotation.getAnnotatedInterface().getId())) {
				for (PropertyOverride<?> override : annotation.getOverrides()) {
					overrideHttpSampler(sampler, override);
				}
			}
		}

		return true;
	}

	private void overrideHttpSampler(HTTPSamplerProxy sampler, PropertyOverride<?> override) {
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
