package org.continuity.wessbas.transform.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Endpoint;

import m4jdsl.Request;

/**
 * @author Henning Schulz
 *
 */
public class InputDataTransformer {

	public List<Pair<Input, Parameter>> transform(Request request, Endpoint<?> interf) {
		if (!(interf instanceof HttpEndpoint)) {
			throw new RuntimeException("Transformation of " + interf.getClass() + " is not yet implemented!");
		}

		HttpEndpoint httpInterf = (HttpEndpoint) interf;

		List<Pair<Input, Parameter>> inputList = new ArrayList<>();

		for (m4jdsl.Parameter wParam : request.getParameters()) {
			for (HttpParameter param : httpInterf.getParameters()) {
				String paramKey = param.getName();

				if (param.getParameterType() == HttpParameterType.URL_PART) {
					paramKey = "URL_PART_" + paramKey;
				}

				if (paramKey.equals(wParam.getName())) {
					DirectListInput input = new DirectListInput();
					input.setId("Input_" + param.getId());
					if (wParam.getValue() != null) {
						input.setData(Arrays.asList(wParam.getValue().split(";")));
					}
					inputList.add(new Pair<>(input, param));
				}
			}
		}

		return inputList;
	}

	public static class InputParamPair {

		private final Input input;
		private final Parameter parameter;

		public InputParamPair(Input input, Parameter parameter) {
			this.input = input;
			this.parameter = parameter;
		}

		/**
		 * Gets {@link #input}.
		 *
		 * @return {@link #input}
		 */
		public Input getInput() {
			return this.input;
		}

		/**
		 * Gets {@link #parameter}.
		 *
		 * @return {@link #parameter}
		 */
		public Parameter getParameter() {
			return this.parameter;
		}

	}

}
