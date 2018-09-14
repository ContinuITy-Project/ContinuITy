package org.continuity.jmeter.transform;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CounterInput;
import org.continuity.idpa.annotation.DataType;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.JsonInput;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.RawValue;

/**
 * @author Henning Schulz
 *
 */
public class HttpArgumentsAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpArgumentsAnnotator.class);

	private final HttpEndpoint endpoint;

	private final ApplicationAnnotation systemAnnotation;

	private final EndpointAnnotation endpointAnn;

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
		}

		setUrlParameters(sampler);
	}

	private List<HTTPArgument> createArguments(HttpParameterType type) {
		return endpoint.getParameters().stream().filter(param -> type == param.getParameterType()).map(this::createArgument).collect(Collectors.toList());
	}

	private HTTPArgument createArgument(HttpParameter param) {
		HTTPArgument arg = new HTTPArgument();

		arg.setName(param.getName());

		return arg;
	}

	private void annotateArguments(List<HTTPArgument> arguments) {
		for (ParameterAnnotation paramAnn : endpointAnn.getParameterAnnotations()) {
			Parameter param = paramAnn.getAnnotatedParameter().resolve(endpoint);

			if (param instanceof HttpParameter) {
				String paramName = ((HttpParameter) param).getName();

				arguments.stream().filter(arg -> Objects.equals(arg.getName(), paramName)).forEach(arg -> annotateArg(arg, paramAnn));
			} else {
				LOGGER.error("Cannot annotate parameter {} of type {}!", param.getId(), param.getClass());
			}
		}
	}

	private void annotateArg(HTTPArgument arg, ParameterAnnotation paramAnnotation) {
		overrideProperties(arg, systemAnnotation.getOverrides());
		overrideProperties(arg, endpointAnn.getOverrides());
		overrideProperties(arg, paramAnnotation.getOverrides());

		arg.setValue(getInputString(paramAnnotation.getInput()));
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
				path = path.replace("{" + paramName + "}", getInputString(paramAnn.getInput()));
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
				case TYPE:
					throw new RuntimeException("Overriding the parameter type in JMeter is not yet implemented!");
				default:
					break;
				}
			}
		}
	}

	/**
	 * Serializes input into a jmeter compatible string.
	 *
	 * @param input
	 *            The input, which has to be serialized
	 * @return input string
	 */
	private String getInputString(Input input) {
		ObjectMapper mapper = new ObjectMapper();
		if ((input instanceof ExtractedInput) || (input instanceof CounterInput)) {
			return "${" + input.getId() + "}";
		} else if (input instanceof DirectListInput) {
			DirectListInput dataInput = (DirectListInput) input;

			if (dataInput.getData().size() > 1) {
				return "${__GetRandomString(${" + input.getId() + "},;)}";
			} else if (dataInput.getData().size() == 1) {
				return dataInput.getData().get(0);
			} else {
				return "";
			}
		} else if (input instanceof JsonInput) {
			JsonInput jsonInput = (JsonInput) input;

			JsonNodeFactory factory = JsonNodeFactory.instance;
			switch (jsonInput.getType()) {
			case STRING:
				return getInputString(jsonInput.getInput());
			case NUMBER:
				return getInputString(jsonInput.getInput());
			case OBJECT:
				ObjectNode jsonObject = factory.objectNode();
				if (null != jsonInput.getItems()) {
					for (JsonInput nestedInput : jsonInput.getItems()) {
						if (nestedInput.getType().equals(DataType.STRING)) {
							// Value is added with quotes
							jsonObject.set(nestedInput.getName(), new TextNode(getInputString(nestedInput)));
						} else {
							// Value is added without quotes
							jsonObject.putRawValue(nestedInput.getName(), new RawValue(getInputString(nestedInput)));
						}
					}
				}
				try {
					return mapper.writeValueAsString(jsonObject);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				return null;
			case ARRAY:
				ArrayNode arrayNode = factory.arrayNode();
				if (null != jsonInput.getItems()) {
					for (JsonInput nestedInput : jsonInput.getItems()) {
						if (nestedInput.getType().equals(DataType.STRING)) {
							// Value is added with quotes
							arrayNode.add(getInputString(nestedInput));
						} else {
							// Value is added without quotes
							arrayNode.addRawValue(new RawValue(getInputString(nestedInput)));
						}
					}
				}
				try {
					return mapper.writeValueAsString(arrayNode);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				return null;
			default:
				return null;

			}

		} else {

			throw new RuntimeException("Input " + input.getClass().getSimpleName() + " is not implemented for JMeter yet!");
		}
	}

}
