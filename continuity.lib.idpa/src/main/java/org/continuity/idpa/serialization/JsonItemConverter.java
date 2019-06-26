package org.continuity.idpa.serialization;

import java.util.HashMap;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.continuity.idpa.annotation.json.JsonArray;
import org.continuity.idpa.annotation.json.JsonItem;
import org.continuity.idpa.annotation.json.JsonObject;
import org.continuity.idpa.annotation.json.JsonStaticValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdConverter;

public class JsonItemConverter extends StdConverter<JsonNode, JsonItem> {

	@Override
	public JsonItem convert(JsonNode value) {
		switch (value.getNodeType()) {
		case OBJECT:
			ObjectNode objNode = (ObjectNode) value;

			JsonObject obj = new JsonObject();
			obj.setItems(new HashMap<>());

			StreamSupport.stream(Spliterators.spliteratorUnknownSize(objNode.fields(), Spliterator.ORDERED), false).forEach(field -> {
				obj.getItems().put(field.getKey(), convert(field.getValue()));
			});

			return obj;
		case ARRAY:
			ArrayNode arrNode = (ArrayNode) value;

			JsonArray arr = new JsonArray();
			arr.setItems(StreamSupport.stream(Spliterators.spliteratorUnknownSize(arrNode.elements(), Spliterator.ORDERED), false).map(this::convert).collect(Collectors.toList()));

			return arr;
		case BINARY:
		case BOOLEAN:
		case NUMBER:
		case STRING:
			String val = value.asText();
			JsonStaticValue item = new JsonStaticValue();
			item.setValue(val);
			return item;
		case NULL:
		case MISSING:
		case POJO:
		default:
			break;

		}

		return null;
	}

}
