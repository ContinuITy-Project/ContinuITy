package org.continuity.jmeter.transform;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.CombinedInput;
import org.continuity.idpa.annotation.CounterInput;
import org.continuity.idpa.annotation.DataType;
import org.continuity.idpa.annotation.DatetimeInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.RandomNumberInput;
import org.continuity.idpa.annotation.RandomStringInput;
import org.continuity.idpa.annotation.json.JsonInput;
import org.continuity.idpa.annotation.json.JsonItem;
import org.continuity.idpa.serialization.json.IdpaSerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.RawValue;

/**
 *
 * @author Tobias Angerstein, Henning Schulz
 *
 */
public class InputFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFormatter.class);

	/**
	 * Serializes input into a jmeter compatible string.
	 *
	 * @param input
	 *            The input, which has to be serialized
	 * @return input string
	 */
	public String getInputString(Input input) {
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

			if (jsonInput.isLegacy()) {
				return formatLegacyJsonInput(jsonInput);
			} else {
				return formatJsonInput(jsonInput);
			}
		} else if (input instanceof RandomNumberInput) {
			RandomNumberInput randNumInput = (RandomNumberInput) input;

			String lower;
			if (randNumInput.lowerIsStatic()) {
				lower = Integer.toString(randNumInput.getStaticLowerLimit());
			} else {
				lower = getInputString(randNumInput.getDerivedLowerLimit());
			}

			String upper;
			if (randNumInput.upperIsStatic()) {
				upper = Integer.toString(randNumInput.getStaticUpperLimit());
			} else {
				upper = getInputString(randNumInput.getDerivedUpperLimit());
			}

			return "${__Random(" + lower + "," + upper + ",)}";
		} else if (input instanceof RandomStringInput) {
			return formatRandomString((RandomStringInput) input);
		} else if (input instanceof DatetimeInput) {
			DatetimeInput datetimeInput = ((DatetimeInput) input);

			if (datetimeInput.getOffset() != null) {
				// If we switch to 3.3 or later, we can use
				// ${__timeShift(datetimeInput.getFormat(),,datetimeInput.getOffset(),,)}
				LOGGER.warn("Date offset ({}) is not supported in current JMeter version! Version 3.3 required.", datetimeInput.getOffset());
			}

			return "${__time(" + datetimeInput.getFormat() + ",)}";
		} else if (input instanceof CombinedInput) {
			CombinedInput combinedInput = (CombinedInput) input;
			String inputString = combinedInput.getFormat();

			int i = 1;
			for (String part : combinedInput.getInputs().stream().map(this::getInputString).collect(Collectors.toList())) {
				inputString = inputString.replace("(" + i + ")", part);
				i++;
			}

			return inputString;
		} else {
			throw new RuntimeException("Input " + input.getClass().getSimpleName() + " is not implemented for JMeter yet!");
		}
	}

	private String formatRandomString(RandomStringInput input) {
		Matcher matcher = Pattern.compile("(\\[[^\\[\\]]*\\])\\{([0-9]+)\\}").matcher(input.getTemplate());

		StringBuilder builder = new StringBuilder();

		int lastEnd = 0;

		while (matcher.find()) {
			int start = matcher.start(0);

			if (start > lastEnd) {
				builder.append(input.getTemplate().substring(lastEnd, start).replace("\\", ""));
			}

			lastEnd = matcher.end(0);

			builder.append("${__RandomString(");
			builder.append(matcher.group(2));
			builder.append(",");

			for (char c = '!'; c <= '~'; c++) {
				if (Character.toString(c).matches(matcher.group(1))) {
					builder.append(c);
				}
			}

			builder.append(",)}");
		}

		if (input.getTemplate().length() > lastEnd) {
			builder.append(input.getTemplate().substring(lastEnd, input.getTemplate().length()).replace("\\", ""));
		}

		return builder.toString();
	}

	private String formatLegacyJsonInput(JsonInput jsonInput) {
		ObjectMapper mapper = IdpaSerializationUtils.getDefaultJsonObjectMapper();

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
				LOGGER.error("Error during formatting of legacy JsonInput", e);
			}
			return null;
		default:
			return null;

		}
	}

	private String formatJsonInput(JsonInput input) {
		ObjectMapper mapper = IdpaSerializationUtils.getDefaultJsonObjectMapper();

		try {
			return mapper.writeValueAsString(convertJsonItem(input.getJson()));
		} catch (JsonProcessingException e) {
			LOGGER.error("Error during formatting of new JsonInput", e);
			return null;
		}
	}

	private JsonNode convertJsonItem(JsonItem item) {
		JsonNodeFactory factory = JsonNodeFactory.instance;

		switch (item.getType()) {
		case STATIC_VALUE:
			return factory.textNode(item.asStaticValue().getValue());
		case DERIVED_VALUE:
			return factory.textNode(getInputString(item.asDerivedValue().getInput()));
		case OBJECT:
			ObjectNode objectNode = factory.objectNode();

			if (null != item.asObject().getItems()) {
				for (Map.Entry<String, JsonItem> entry : item.asObject().getItems().entrySet()) {
					objectNode.set(entry.getKey(), convertJsonItem(entry.getValue()));
				}
			}

			return objectNode;
		case ARRAY:
			ArrayNode arrayNode = factory.arrayNode();

			if (null != item.asArray().getItems()) {
				for (JsonItem nestedItem : item.asArray().getItems()) {
					arrayNode.add(convertJsonItem(nestedItem));
				}
			}

			return arrayNode;
		default:
			return null;
		}
	}

}
