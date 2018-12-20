package org.continuity.idpa.annotation.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.annotation.DataType;
import org.continuity.idpa.annotation.Input;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * This class represents two implementations - the {@code JsonInput} and the
 * {@code LegacyJsonInput}. For the {@code JsonInput}, the {@link #json} field is to be used. For
 * the {@code LegacyJsonInput}, the other fields are to be used.
 *
 * @author Tobias Angerstein, Henning Schulz
 *
 */
@JsonPropertyOrder({ "type", "name", "input", "items", "json" })
public class JsonInput extends AbstractIdpaElement implements Input {

	@JsonProperty(value = "items", required = false)
	@JsonInclude(Include.NON_EMPTY)
	private List<JsonInput> items;

	@JsonProperty(value = "type", required = false)
	@JsonInclude(Include.NON_NULL)
	private DataType type;

	@JsonProperty(value = "name", required = false)
	@JsonInclude(Include.NON_NULL)
	private String name;

	@JsonProperty(value = "input", required = false)
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	@JsonInclude(Include.NON_NULL)
	private Input input;

	@JsonProperty(value = "json", required = false)
	@JsonInclude(Include.NON_NULL)
	private JsonItem json;

	public List<JsonInput> getItems() {
		if (!isLegacy()) {
			return null;
		}

		if (items == null) {
			items = new ArrayList<>();
		}
		return this.items;
	}

	public void setItems(List<JsonInput> items) {
		this.items = items;
	}

	public DataType getType() {
		if (!isLegacy()) {
			return null;
		}

		return type;
	}

	public void setType(DataType type) {
		this.type = type;
	}

	public Input getInput() {
		if (!isLegacy()) {
			return null;
		}

		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public String getName() {
		if (!isLegacy()) {
			return null;
		}

		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JsonItem getJson() {
		return json;
	}

	public void setJson(JsonItem json) {
		this.json = json;
	}

	@JsonIgnore
	public boolean isLegacy() {
		return this.json == null;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonInput)) {
			return false;
		}

		JsonInput other = (JsonInput) obj;

		return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.items, other.items) && Objects.equals(this.type, other.type) && Objects.equals(this.input, other.input)
				&& Objects.equals(this.name, other.name) && Objects.equals(this.json, other.json);
	}

}
