package org.continuity.jmeter.transform;

import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
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
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.Parameter;

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

	private static final String KEY_URL_PART = "URL_PART_";
	private static final String BODY = "_BODY";

	private final Application system;

	private final ApplicationAnnotation systemAnnotation;

	private final EndpointAnnotation interfAnnotation;

	public HttpArgumentsAnnotator(Application system, ApplicationAnnotation systemAnnotation, EndpointAnnotation interfAnnotation) {
		this.system = system;
		this.systemAnnotation = systemAnnotation;
		this.interfAnnotation = interfAnnotation;
	}

	public void annotateArguments(HTTPSamplerProxy sampler) {
		PropertyIterator it = sampler.getArguments().getArguments().iterator();
		List<HTTPArgument> urlPartArguments = new ArrayList<>();

		while (it.hasNext()) {
			JMeterProperty prop = it.next();
			if ((prop.getObjectValue() instanceof HTTPArgument)) {
				HTTPArgument arg = (HTTPArgument) prop.getObjectValue();

				if (arg.getName().startsWith(KEY_URL_PART)) {
					urlPartArguments.add(arg);
				} else if (arg.getName().startsWith(BODY)) {
					sampler.setPostBodyRaw(true);
					arg.setName("body");
					annotateArg(arg);
				} else {
					annotateArg(arg);
				}
			}
		}

		String path = sampler.getPath();

		for (HTTPArgument arg : urlPartArguments) {
			sampler.getArguments().removeArgument(arg);

			String paramName = arg.getName().substring(KEY_URL_PART.length());
			ParameterAnnotation paramAnnotation = findAnnotationForParameterName(paramName);
			path = path.replace("{" + paramName + "}", getInputString(paramAnnotation.getInput()));
		}

		sampler.setPath(path);
	}

	private void annotateArg(HTTPArgument arg) {
		overrideProperties(arg, systemAnnotation.getOverrides());
		overrideProperties(arg, interfAnnotation.getOverrides());
		ParameterAnnotation paramAnnotation = findAnnotationForParameterName(arg.getName());

		if (paramAnnotation != null) {
			overrideProperties(arg, paramAnnotation.getOverrides());

			// TODO: clear all arguments and create all from scratch based on
			// ParameterAnnotations and overrides
			arg.setValue(getInputString(paramAnnotation.getInput()));
		}
	}

	private ParameterAnnotation findAnnotationForParameterName(String paramName) {
		for (ParameterAnnotation paramAnnotation : interfAnnotation.getParameterAnnotations()) {
			Parameter param = paramAnnotation.getAnnotatedParameter().resolve(system);
			if (!(param instanceof HttpParameter)) {
				continue;
			}

			HttpParameter httpParam = (HttpParameter) param;

			if (paramName.equals(httpParam.getName())) {
				return paramAnnotation;
			}
		}

		return null;
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
