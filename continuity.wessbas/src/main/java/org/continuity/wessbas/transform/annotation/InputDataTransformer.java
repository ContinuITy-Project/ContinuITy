package org.continuity.wessbas.transform.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.continuity.annotation.dsl.ann.DirectDataInput;
import org.continuity.annotation.dsl.ann.Input;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;

import m4jdsl.Request;

/**
 * @author Henning Schulz
 *
 */
public class InputDataTransformer {

	public List<Pair<Input, Parameter>> transform(Request request, ServiceInterface<?> interf) {
		if (!(interf instanceof HttpInterface)) {
			throw new RuntimeException("Transformation of " + interf.getClass() + " is not yet implemented!");
		}

		HttpInterface httpInterf = (HttpInterface) interf;

		List<Pair<Input, Parameter>> inputList = new ArrayList<>();

		for (m4jdsl.Parameter wParam : request.getParameters()) {
			for (HttpParameter param : httpInterf.getParameters()) {
				// TODO: consider BODY and URL_PART, too
				if (param.getName().equals(wParam.getName())) {
					DirectDataInput input = new DirectDataInput();
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
