package org.continuity.idpa.annotation.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public class JsonStaticValue implements JsonItem {

	@JsonValue
	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public Type getType() {
		return Type.STATIC_VALUE;
	}

	@Override
	public JsonObject asObject() {
		throw new ClassCastException("Cannot cast a JsonStaticValue to JsonObject!");
	}

	@Override
	public JsonArray asArray() {
		throw new ClassCastException("Cannot cast a JsonStaticValue to JsonArray!");
	}

	@Override
	public JsonStaticValue asStaticValue() {
		return this;
	}

	@Override
	public JsonDerivedValue asDerivedValue() {
		throw new ClassCastException("Cannot cast a JsonStaticValue to JsonDerivedValue!");
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonStaticValue)) {
			return false;
		}

		JsonStaticValue other = (JsonStaticValue) obj;

		return Objects.equals(this.value, other.value);
	}

}
