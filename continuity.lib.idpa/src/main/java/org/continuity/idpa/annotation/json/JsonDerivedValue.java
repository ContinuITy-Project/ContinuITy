package org.continuity.idpa.annotation.json;

import java.util.Objects;

import org.continuity.idpa.annotation.Input;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

public class JsonDerivedValue implements JsonItem {

	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	@JsonValue
	private Input input;

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	@Override
	public Type getType() {
		return Type.DERIVED_VALUE;
	}

	@Override
	public JsonObject asObject() {
		throw new ClassCastException("Cannot cast a JsonDerivedValue to JsonObject!");
	}

	@Override
	public JsonArray asArray() {
		throw new ClassCastException("Cannot cast a JsonDerivedValue to JsonArray!");
	}

	@Override
	public JsonStaticValue asStaticValue() {
		throw new ClassCastException("Cannot cast a JsonDerivedValue to JsonStaticValue!");
	}

	@Override
	public JsonDerivedValue asDerivedValue() {
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonDerivedValue)) {
			return false;
		}

		JsonDerivedValue other = (JsonDerivedValue) obj;

		return ((this.input == null) && (other.input == null)) || Objects.equals(this.input.getId(), other.input.getId());
	}

}
