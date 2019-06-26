package org.continuity.idpa.annotation.json;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public class JsonObject implements JsonItem {

	@JsonValue
	private Map<String, JsonItem> items;

	public Map<String, JsonItem> getItems() {
		return items;
	}

	public void setItems(Map<String, JsonItem> items) {
		this.items = items;
	}

	@Override
	public Type getType() {
		return Type.OBJECT;
	}

	@Override
	public JsonObject asObject() {
		return this;
	}

	@Override
	public JsonArray asArray() {
		throw new ClassCastException("Cannot cast a JsonObject to JsonArray!");
	}

	@Override
	public JsonStaticValue asStaticValue() {
		throw new ClassCastException("Cannot cast a JsonObject to JsonStaticValue!");
	}

	@Override
	public JsonDerivedValue asDerivedValue() {
		throw new ClassCastException("Cannot cast a JsonObject to JsonDerivedValue!");
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonObject)) {
			return false;
		}

		JsonObject other = (JsonObject) obj;

		if (this.items == null) {
			return other.items == null;
		} else if (other.items == null) {
			return false;
		} else {
			return (this.items.size() == other.items.size())
					&& this.items.entrySet().stream().map(entry -> Objects.equals(entry.getValue(), other.items.get(entry.getKey()))).reduce(Boolean::logicalAnd).orElseGet(() -> true);
		}
	}

}
