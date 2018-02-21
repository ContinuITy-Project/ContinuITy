package org.continuity.jmeter.transform;

import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.continuity.annotation.dsl.ann.DirectDataInput;
import org.continuity.annotation.dsl.ann.ExtractedInput;
import org.continuity.annotation.dsl.ann.Input;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.ann.PropertyOverride;
import org.continuity.annotation.dsl.ann.PropertyOverrideKey;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.SystemModel;

/**
 * @author Henning Schulz
 *
 */
public class HttpArgumentsAnnotator {

	private static final String KEY_URL_PART = "URL_PART_";

	private final SystemModel system;

	private final SystemAnnotation systemAnnotation;

	private final InterfaceAnnotation interfAnnotation;

	public HttpArgumentsAnnotator(SystemModel system, SystemAnnotation systemAnnotation, InterfaceAnnotation interfAnnotation) {
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

	private String getInputString(Input input) {
		if (input instanceof ExtractedInput) {
			return "${" + input.getId() + "}";
		} else if (input instanceof DirectDataInput) {
			DirectDataInput dataInput = (DirectDataInput) input;

			if (dataInput.getData().size() > 1) {
				return "${__GetRandomString(${" + input.getId() + "},;)}";
			} else if (dataInput.getData().size() == 1) {
				return dataInput.getData().get(0);
			} else {
				return "";
			}
		} else {
			throw new RuntimeException("Input " + input.getClass().getSimpleName() + " is not implemented for JMeter yet!");
		}
	}

}
