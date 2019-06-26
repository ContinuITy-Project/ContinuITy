package org.continuity.orchestrator.entities;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

public class SetKeyDeserializer extends KeyDeserializer {

	@Override
	public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
		if (!key.matches("\\[.*\\]")) {
			throw new IOException("Cannot deserialize " + key + " to Set<String>!");
		}

		return Arrays.stream(key.substring(1, key.length() - 1).split("\\,")).map(String::trim).collect(Collectors.toSet());
	}

}
