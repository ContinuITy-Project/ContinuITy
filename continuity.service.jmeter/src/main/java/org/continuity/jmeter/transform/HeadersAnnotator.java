package org.continuity.jmeter.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Schulz
 *
 */
public class HeadersAnnotator extends AbstractSamplerAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeadersAnnotator.class);

	private final InputFormatter inputFormatter = new InputFormatter();

	protected HeadersAnnotator(Application system, ApplicationAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSampler(HTTPSamplerProxy sampler, HttpEndpoint endpoint, EndpointAnnotation annotation, HashTree samplerTree) {
		HeaderPanel headerGui = new HeaderPanel();
		HeaderManager headerManager = (HeaderManager) headerGui.createTestElement();
		samplerTree.getTree(sampler).add(new ListedHashTree(headerManager));

		addHeaders(headerManager, endpoint.getHeaders());

		overrideHeaders(headerManager, getAnnotation().getOverrides());
		overrideHeaders(headerManager, annotation.getOverrides());

		addHeaderParameters(headerManager, endpoint, annotation);
	}

	private void addHeaders(HeaderManager headerManager, Iterable<String> headers) {
		for (String header : headers) {
			addHeader(headerManager, header);
		}
	}

	private <T extends PropertyOverrideKey.Any> void overrideHeaders(HeaderManager headerManager, List<PropertyOverride<T>> overrides) {
		for (PropertyOverride<?> override : overrides) {
			if (override.getKey() == PropertyOverrideKey.HttpEndpoint.HEADER) {
				addHeader(headerManager, override.getValue());
			}
		}
	}

	private void addHeaderParameters(HeaderManager headerManager, HttpEndpoint endpoint, EndpointAnnotation annotation) {
		for (ParameterAnnotation paramAnn : annotation.getParameterAnnotations()) {
			Parameter param = paramAnn.getAnnotatedParameter().resolve(endpoint);

			if (param == null) {
				LOGGER.error("Illegal header parameter annotation! There is no parameter '{}'.", paramAnn.getAnnotatedParameter().getId());
			} else if ((param instanceof HttpParameter) && (((HttpParameter) param).getParameterType() == HttpParameterType.HEADER)) {
				addHeader(headerManager, ((HttpParameter) param).getName(), inputFormatter.getInputString(paramAnn.getInput()));
			}
		}

		endpoint.getParameters().stream().filter(param -> HttpParameterType.HEADER == param.getParameterType()).forEach(param -> {

		});
	}

	private void addHeader(HeaderManager headerManager, String header) {
		String[] headerParts = header.split("\\:");

		if (headerParts.length == 2) {
			addHeader(headerManager, headerParts[0].trim(), headerParts[1].trim());
		} else {
			LOGGER.error("Cannot add header {}! Splitting by : resulted in {} parts.", header, headerParts.length);
		}
	}

	private void addHeader(HeaderManager headerManager, String key, String value) {
		removeHeaderWithKey(headerManager, key);
		headerManager.add(new Header(key, value));
	}

	private void removeHeaderWithKey(HeaderManager headerManager, String key) {
		List<Integer> toRemove = new ArrayList<>();

		for (int i = 0; i < headerManager.size(); i++) {
			if (Objects.equals(headerManager.get(i).getName(), key)) {
				toRemove.add(i);
			}
		}

		ListIterator<Integer> it = toRemove.listIterator();

		while (it.hasPrevious()) {
			headerManager.remove(it.previous());
		}
	}

}
