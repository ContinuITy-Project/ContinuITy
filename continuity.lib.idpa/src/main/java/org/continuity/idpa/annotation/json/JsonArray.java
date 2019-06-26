package org.continuity.idpa.annotation.json;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public class JsonArray implements JsonItem {

	@JsonValue
	private List<JsonItem> items;

	public List<JsonItem> getItems() {
		return items;
	}

	public void setItems(List<JsonItem> items) {
		this.items = items;
	}

	@Override
	public Type getType() {
		return Type.ARRAY;
	}

	@Override
	public JsonObject asObject() {
		throw new ClassCastException("Cannot cast a JsonArray to JsonObject!");
	}

	@Override
	public JsonArray asArray() {
		return this;
	}

	@Override
	public JsonStaticValue asStaticValue() {
		throw new ClassCastException("Cannot cast a JsonArray to JsonStaticValue!");
	}

	@Override
	public JsonDerivedValue asDerivedValue() {
		throw new ClassCastException("Cannot cast a JsonArray to JsonDerivedValue!");
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonArray)) {
			return false;
		}

		JsonArray other = (JsonArray) obj;

		return Objects.equals(this.items, other.items);
	}

}
