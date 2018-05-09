package org.continuity.jmeter.transform;

import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Schulz
 *
 */
public class HeadersAnnotator extends AbstractSamplerAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeadersAnnotator.class);

	protected HeadersAnnotator(Application system, ApplicationAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSamplerBySystemAnnotation(HTTPSamplerProxy sampler, ApplicationAnnotation annotation, HashTree samplerTree) {
		// TODO: add global overrides to global header manager
	}

	@Override
	protected void annotateHttpSamplerByInterfaceAnnotation(HTTPSamplerProxy sampler, EndpointAnnotation annotation, HashTree samplerTree) {
		HeaderPanel headerGui = new HeaderPanel();
		HeaderManager headerManager = (HeaderManager) headerGui.createTestElement();
		samplerTree.getTree(sampler).add(new ListedHashTree(headerManager));

		Endpoint<?> interf = annotation.getAnnotatedEndpoint().getReferred();

		if (interf instanceof HttpEndpoint) {
			HttpEndpoint httpInterf = (HttpEndpoint) interf;
			addHeaders(headerManager, httpInterf.getHeaders());
		} else {
			LOGGER.error("Cannot add headers from interface {}, since it is not an HttpInterface (but {})!", interf.getId(), interf.getClass().getSimpleName());
		}

		// TODO add headers from interface overrides

	}

	private void addHeaders(HeaderManager headerManager, Iterable<String> headers) {
		for (String header : headers) {
			String[] headerParts = header.split("\\:");

			if (headerParts.length == 2) {
				headerManager.add(new Header(headerParts[0].trim(), headerParts[1].trim()));
			} else {
				LOGGER.error("Cannot add header {}! Splitting is by : resulted in {} parts.", header, headerParts.length);
			}
		}
	}

}
