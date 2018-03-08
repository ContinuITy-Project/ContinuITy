package org.continuity.jmeter.transform;

import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Schulz
 *
 */
public class HeadersAnnotator extends AbstractSamplerAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeadersAnnotator.class);

	protected HeadersAnnotator(SystemModel system, SystemAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSamplerBySystemAnnotation(HTTPSamplerProxy sampler, SystemAnnotation annotation, HashTree samplerTree) {
		// TODO: add global overrides to global header manager
	}

	@Override
	protected void annotateHttpSamplerByInterfaceAnnotation(HTTPSamplerProxy sampler, InterfaceAnnotation annotation, HashTree samplerTree) {
		HeaderPanel headerGui = new HeaderPanel();
		HeaderManager headerManager = (HeaderManager) headerGui.createTestElement();
		samplerTree.getTree(sampler).add(new ListedHashTree(headerManager));

		ServiceInterface<?> interf = annotation.getAnnotatedInterface().getReferred();

		if (interf instanceof HttpInterface) {
			HttpInterface httpInterf = (HttpInterface) interf;
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
