package org.continuity.jmeter.transform;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Henning Schulz
 *
 */
public class HttpArgumentsAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpArgumentsAnnotator.class);

	private static final String KEY_ID = "Continuity.id";

	private final HttpEndpoint endpoint;

	private final ApplicationAnnotation systemAnnotation;

	private final EndpointAnnotation endpointAnn;

	private final InputFormatter inputFormatter = new InputFormatter();

	public HttpArgumentsAnnotator(HttpEndpoint endpoint, ApplicationAnnotation systemAnnotation, EndpointAnnotation interfAnnotation) {
		this.endpoint = endpoint;
		this.systemAnnotation = systemAnnotation;
		this.endpointAnn = interfAnnotation;
	}

	public void annotateArguments(HTTPSamplerProxy sampler) {
		Arguments arguments = sampler.getArguments();
		arguments.clear();

		List<HTTPArgument> bodyArgs = createArguments(HttpParameterType.BODY);
		List<HTTPArgument> formArgs = createArguments(HttpParameterType.FORM);
		List<HTTPArgument> queryArgs = createArguments(HttpParameterType.REQ_PARAM);

		annotateArguments(bodyArgs);
		annotateArguments(formArgs);
		annotateArguments(queryArgs);

		if (bodyArgs.size() > 1) {
			LOGGER.error("There are {} bodies for endpoint {}! Only using the first!", bodyArgs.size(), endpoint.getId());
		}

		if (bodyArgs.size() >= 1) {
			if (formArgs.size() > 0) {
				LOGGER.error("Cannot define body and form parameters at the same time! Ignoring form parameters {}.", formArgs.stream().map(HTTPArgument::getName).collect(Collectors.toList()));
			}

			sampler.setPostBodyRaw(true);
			arguments.addArgument(bodyArgs.get(0));

			addArgumentsToPath(sampler, queryArgs);
		} else if (formArgs.size() > 0) {
			sampler.setPostBodyRaw(false);
			sampler.setDoMultipartPost(true);

			formArgs.forEach(arguments::addArgument);

			addArgumentsToPath(sampler, queryArgs);
		} else {
			sampler.setPostBodyRaw(false);
			sampler.setDoMultipartPost(false);

			queryArgs.forEach(arguments::addArgument);
			queryArgs.forEach(argument -> argument.setUseEquals(true));

		}

		setUrlParameters(sampler);
	}

	private List<HTTPArgument> createArguments(HttpParameterType type) {
		return endpoint.getParameters().stream().filter(param -> type == param.getParameterType()).map(this::createArgument).collect(Collectors.toList());
	}

	private HTTPArgument createArgument(HttpParameter param) {
		HTTPArgument arg = new HTTPArgument();

		if ((param.getParameterType() == HttpParameterType.BODY) && (param.getName() == null)) {
			arg.setName("body");
		} else if (param.getName() == null) {
			arg.setName("UNDEFINED");
		} else {
			arg.setName(param.getName());
		}

		arg.setProperty(KEY_ID, param.getId());

		return arg;
	}

	private void annotateArguments(List<HTTPArgument> arguments) {
		for (ParameterAnnotation paramAnn : endpointAnn.getParameterAnnotations()) {
			Parameter param = paramAnn.getAnnotatedParameter().resolve(endpoint);

			if (param instanceof HttpParameter) {
				arguments.stream().filter(arg -> Objects.equals(arg.getProperty(KEY_ID).getStringValue(), param.getId())).forEach(arg -> annotateArg(arg, paramAnn));
			} else {
				LOGGER.error("Cannot annotate parameter {} of type {}!", param.getId(), param.getClass());
			}
		}
	}

	private void annotateArg(HTTPArgument arg, ParameterAnnotation paramAnnotation) {
		overrideProperties(arg, systemAnnotation.getOverrides());
		overrideProperties(arg, endpointAnn.getOverrides());
		overrideProperties(arg, paramAnnotation.getOverrides());

		arg.setValue(inputFormatter.getInputString(paramAnnotation.getInput()));
	}

	private void addArgumentsToPath(HTTPSamplerProxy sampler, List<HTTPArgument> arguments) {
		if (arguments.size() > 0) {
			StringBuilder builder = new StringBuilder();

			arguments.forEach(arg -> {
				builder.append("&");
				builder.append(arg.getName());
				builder.append("=");
				builder.append(arg.getValue());
			});

			builder.replace(0, 1, "?");

			sampler.setPath(sampler.getPath() + builder.toString());
		}
	}

	private void setUrlParameters(HTTPSamplerProxy sampler) {
		String path = sampler.getPath();

		for (ParameterAnnotation paramAnn : endpointAnn.getParameterAnnotations()) {
			Parameter param = paramAnn.getAnnotatedParameter().resolve(endpoint);

			if ((param instanceof HttpParameter) && (((HttpParameter) param).getParameterType() == HttpParameterType.URL_PART)) {
				String paramName = ((HttpParameter) param).getName();
				path = path.replace("{" + paramName + "}", inputFormatter.getInputString(paramAnn.getInput()));
			}
		}

		sampler.setPath(path);
	}

	private <T extends PropertyOverrideKey.Any> void overrideProperties(HTTPArgument arg, List<PropertyOverride<T>> overrides) {
		for (PropertyOverride<?> override : overrides) {
			if (override.getKey().isInScope(PropertyOverrideKey.HttpParameter.class)) {
				switch ((PropertyOverrideKey.HttpParameter) override.getKey()) {
				case ENCODED:
					arg.setAlwaysEncoded(true);
					break;
				default:
					break;
				}
			}
		}
	}

}
