package org.continuity.idpa.annotation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.idpa.AbstractIdpaElement;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.RawValue;

@JsonPropertyOrder({"type"})
public class JsonInput extends AbstractIdpaElement implements Input {

	@JsonProperty(value = "items", required = false)
	private List<JsonInput> items;

	@JsonProperty(value = "type", required = true)
	private DataType type;
	
	@JsonProperty(value = "name", required = false)
	private String name;

	@JsonProperty(value = "input")
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	private Input input;

	public List<JsonInput> getItems() {
		if (items == null) {
			items = new ArrayList<>();
		}
		return this.items;
	}

	public void setItems(List<JsonInput> items) {
		this.items = items;
	}

	public DataType getType() {
		return type;
	}

	public void setType(DataType type) {
		this.type = type;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
