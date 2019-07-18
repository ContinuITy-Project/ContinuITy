package org.continuity.api.entities.deserialization;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.continuity.idpa.AppId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class CustomMapDeserializer extends StdDeserializer<Map<AppId, String>> {

	/**
	 * Serialization VersionUID
	 */
	private static final long serialVersionUID = 1L;

	public CustomMapDeserializer() {
		this(null);
	}

	public CustomMapDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public Map<AppId, String> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);
		if (node.isArray()) {
			ArrayNode arrayNode = (ArrayNode) node;
			return StreamSupport.stream(arrayNode.spliterator(), false).map(e -> new SimpleEntry<>(AppId.fromString(e.textValue()), "undefined"))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(node.toString(), new TypeReference<Map<AppId, String>>() {
		});
	}
}