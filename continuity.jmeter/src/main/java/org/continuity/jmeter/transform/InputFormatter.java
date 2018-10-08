package org.continuity.jmeter.transform;

import org.continuity.idpa.annotation.CounterInput;
import org.continuity.idpa.annotation.DataType;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.JsonInput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.RawValue;

public class InputFormatter {

	/**
	 * Serializes input into a jmeter compatible string.
	 *
	 * @param input
	 *            The input, which has to be serialized
	 * @return input string
	 */
	public String getInputString(Input input) {
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
