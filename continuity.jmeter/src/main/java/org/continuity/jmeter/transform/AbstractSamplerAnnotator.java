package org.continuity.jmeter.transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

public abstract class AbstractSamplerAnnotator {

	private static final Pattern REQUEST_PATTERN = Pattern.compile("R\\d+\\s\\((.*)\\)");

	private final Application system;

	private final ApplicationAnnotation annotation;

	protected AbstractSamplerAnnotator(Application system, ApplicationAnnotation annotation) {
		this.system = system;
		this.annotation = annotation;
	}

	protected abstract void annotateHttpSamplerBySystemAnnotation(HTTPSamplerProxy sampler, ApplicationAnnotation annotation, HashTree samplerTree);

	protected abstract void annotateHttpSamplerByInterfaceAnnotation(HTTPSamplerProxy sampler, EndpointAnnotation annotation, HashTree samplerTree);

	protected Application getSystem() {
		return system;
	}

	protected ApplicationAnnotation getAnnotation() {
		return annotation;
	}

	public void annotateSamplers(ListedHashTree testPlan) {
		IdpaByClassSearcher<EndpointAnnotation> searcher = new IdpaByClassSearcher<>(EndpointAnnotation.class, e -> annotateInterface(e, testPlan));
		searcher.visit(annotation);
	}

	private void annotateInterface(IdpaElement element, ListedHashTree testPlan) {
		EndpointAnnotation annotation = (EndpointAnnotation) element;

		Endpoint<?> interf = annotation.getAnnotatedEndpoint().resolve(system);

		if (interf instanceof HttpEndpoint) {
			annotateHttpInterface(annotation, testPlan);
		} else {
			throw new RuntimeException("Annotation of " + interf.getClass().getSimpleName() + " in JMeter is not yet implemented!");
		}
	}

	private void annotateHttpInterface(EndpointAnnotation interfAnnotation, ListedHashTree testPlan) {
		SearchByClass<HTTPSamplerProxy> search = new SearchByClass<>(HTTPSamplerProxy.class);
		testPlan.traverse(search);

		for (HTTPSamplerProxy sampler : search.getSearchResults()) {
			HashTree samplerTree = search.getSubTree(sampler);
			annotateHttpSamplerBySystemAnnotation(sampler, annotation, samplerTree);

			if (areEqualRequests(interfAnnotation.getAnnotatedEndpoint().getId(), sampler.getName())) {
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
