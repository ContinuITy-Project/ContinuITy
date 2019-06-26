package org.continuity.jmeter.transform;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.visitor.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSamplerAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSamplerAnnotator.class);

	private static final Pattern REQUEST_PATTERN = Pattern.compile("R\\d+\\s\\((.*)\\)");

	private final Application system;

	private final ApplicationAnnotation annotation;

	protected AbstractSamplerAnnotator(Application system, ApplicationAnnotation annotation) {
		this.system = system;
		this.annotation = annotation;
	}

	protected abstract void annotateHttpSampler(HTTPSamplerProxy sampler, HttpEndpoint endpoint, EndpointAnnotation annotation, HashTree samplerTree);

	protected Application getSystem() {
		return system;
	}

	protected ApplicationAnnotation getAnnotation() {
		return annotation;
	}

	public void annotateSamplers(ListedHashTree testPlan) {
		SearchByClass<HTTPSamplerProxy> search = new SearchByClass<>(HTTPSamplerProxy.class);
		testPlan.traverse(search);

		for (HTTPSamplerProxy sampler : search.getSearchResults()) {
			HashTree samplerTree = search.getSubTree(sampler);

			String endpointId = extractRequestName(sampler.getName());
			
			EndpointAnnotation ann = null;
			
			// remove prefix
			if(endpointId.contains("#")) {
				ann = FindBy.find(a -> Objects.equals(a.getAnnotatedEndpoint().getId(), endpointId.split("#")[1]), EndpointAnnotation.class).in(getAnnotation()).getFound();
			} else {
				ann = FindBy.find(a -> Objects.equals(a.getAnnotatedEndpoint().getId(), endpointId), EndpointAnnotation.class).in(getAnnotation()).getFound();
			}

			if (ann == null) {
				LOGGER.warn("No endpoint annotation found for endpoint {}!", endpointId);

				// Using an empty annotation for convenience
				ann = new EndpointAnnotation();
				ann.setAnnotatedEndpoint(WeakReference.create(Endpoint.GENERIC_TYPE, endpointId));
			}

			Endpoint<?> endpoint = ann.getAnnotatedEndpoint().resolve(getSystem());

			if ((endpoint != null) && (endpoint instanceof HttpEndpoint)) {
				annotateHttpSampler(sampler, (HttpEndpoint) endpoint, ann, samplerTree);
			} else {
				LOGGER.warn("No HttpEndpoint found for ID {}. Leaving sampler {} as it is.", endpointId, sampler.getName());
			}
		}
	}

	private String extractRequestName(String jmeterSamplerName) {
		Matcher matcher = REQUEST_PATTERN.matcher(jmeterSamplerName);

		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return jmeterSamplerName;
		}
	}

}
