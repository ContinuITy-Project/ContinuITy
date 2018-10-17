package org.continuity.jmeter.transform;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;

/**
 * @author Henning Schulz
 *
 */
public class HttpSamplersAnnotator extends AbstractSamplerAnnotator {

	public HttpSamplersAnnotator(Application system, ApplicationAnnotation annotation) {
		super(system, annotation);
	}

	@Override
	protected void annotateHttpSampler(HTTPSamplerProxy sampler, HttpEndpoint endpoint, EndpointAnnotation annotation, HashTree samplerTree) {
		updateSamplerProperties(sampler, endpoint);

		overrideSamplerProperties(sampler, endpoint, getAnnotation().getOverrides());
		overrideSamplerProperties(sampler, endpoint, annotation.getOverrides());

		annotateParameters(sampler, endpoint, annotation);
	}

	private void updateSamplerProperties(HTTPSamplerProxy sampler, HttpEndpoint endpoint) {
		sampler.setName(endpoint.getId());

		setIfNotNull(sampler::setDomain, endpoint.getDomain());
		sampler.setPort(endpoint.getPort() == null ? 80 : Integer.parseInt(endpoint.getPort()));
		setIfNotNull(sampler::setProtocol, endpoint.getProtocol());
		setIfNotNull(sampler::setMethod, endpoint.getMethod());
		setIfNotNull(sampler::setPath, endpoint.getPath());

		if (!Objects.equals(HttpEndpoint.DEFAULT_ENCODING, endpoint.getEncoding())) {
			setIfNotNull(sampler::setContentEncoding, endpoint.getEncoding());
		}

		sampler.setAutoRedirects(false);
		sampler.setFollowRedirects(true);
		sampler.setUseKeepAlive(true);
		sampler.setDoBrowserCompatibleMultipart(false);
	}

	private <T extends PropertyOverrideKey.Any> void overrideSamplerProperties(HTTPSamplerProxy sampler, HttpEndpoint endpoint, List<PropertyOverride<T>> overrides) {
		for (PropertyOverride<?> override : overrides) {
			if (override.getKey().isInScope(PropertyOverrideKey.HttpEndpoint.class)) {
				switch ((PropertyOverrideKey.HttpEndpoint) override.getKey()) {
				case DOMAIN:
					sampler.setDomain(override.resultingValue(endpoint));
					break;
				case ENCODING:
					sampler.setContentEncoding(override.resultingValue(endpoint));
					break;
				case PORT:
					sampler.setPort(Integer.parseInt(override.resultingValue(endpoint)));
					break;
				case PROTOCOL:
					sampler.setProtocol(override.resultingValue(endpoint));
					break;
				case BASE_PATH:
					sampler.setPath(override.resultingValue(endpoint));
					break;
				default:
					// do nothing
					break;
				}
			}
		}
	}

	private void annotateParameters(HTTPSamplerProxy sampler, HttpEndpoint endpoint, EndpointAnnotation annotation) {
		new HttpArgumentsAnnotator(endpoint, getAnnotation(), annotation).annotateArguments(sampler);
	}

	private <T> void setIfNotNull(Consumer<T> setter, T value) {
		if (value != null) {
			setter.accept(value);
		}
	}

}
