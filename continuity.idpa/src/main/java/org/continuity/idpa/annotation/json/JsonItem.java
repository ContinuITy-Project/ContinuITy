package org.continuity.idpa.annotation.json;

import org.continuity.idpa.serialization.JsonItemConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(converter = JsonItemConverter.class)
public interface JsonItem {

	@JsonIgnore
	Type getType();

	JsonObject asObject();

	JsonArray asArray();

	JsonStaticValue asStaticValue();

	JsonDerivedValue asDerivedValue();

	public static enum Type {
		OBJECT, ARRAY, STATIC_VALUE, DERIVED_VALUE
	}

}
