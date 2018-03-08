package org.continuity.jmeter.transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.annotation.dsl.ContinuityModelElement;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;

public abstract class AbstractSamplerAnnotator {

	private static final Pattern REQUEST_PATTERN = Pattern.compile("R\\d+\\s\\((.*)\\)");

	private final SystemModel system;

	private final SystemAnnotation annotation;

	protected AbstractSamplerAnnotator(SystemModel system, SystemAnnotation annotation) {
		this.system = system;
		this.annotation = annotation;
	}

	protected abstract void annotateHttpSamplerBySystemAnnotation(HTTPSamplerProxy sampler, SystemAnnotation annotation, HashTree samplerTree);

	protected abstract void annotateHttpSamplerByInterfaceAnnotation(HTTPSamplerProxy sampler, InterfaceAnnotation annotation, HashTree samplerTree);

	protected SystemModel getSystem() {
		return system;
	}

	protected SystemAnnotation getAnnotation() {
		return annotation;
	}

	public void annotateSamplers(ListedHashTree testPlan) {
		ContinuityByClassSearcher<InterfaceAnnotation> searcher = new ContinuityByClassSearcher<>(InterfaceAnnotation.class, e -> annotateInterface(e, testPlan));
		searcher.visit(annotation);
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

	private void annotateHttpInterface(InterfaceAnnotation interfAnnotation, ListedHashTree testPlan) {
		SearchByClass<HTTPSamplerProxy> search = new SearchByClass<>(HTTPSamplerProxy.class);
		testPlan.traverse(search);

		for (HTTPSamplerProxy sampler : search.getSearchResults()) {
			HashTree samplerTree = search.getSubTree(sampler);
			annotateHttpSamplerBySystemAnnotation(sampler, annotation, samplerTree);

			if (areEqualRequests(interfAnnotation.getAnnotatedInterface().getId(), sampler.getName())) {
				annotateHttpSamplerByInterfaceAnnotation(sampler, interfAnnotation, samplerTree);
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
