package org.continuity.jmeter.transform;

import java.util.Collections;
import java.util.HashMap;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.json.JsonDerivedValue;
import org.continuity.idpa.annotation.json.JsonInput;
import org.continuity.idpa.annotation.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

public class JsonInputFormatterTest {

	private static final String EXPECTED = "{\"derived\":\"foo\"}";

	private ApplicationAnnotation annotation;

	private JsonInput jsonInput;

	@Before
	public void setup() {
		this.annotation = new ApplicationAnnotation();

		DirectListInput listInput = new DirectListInput();
		listInput.setId("Input_list");
		listInput.setData(Collections.singletonList("foo"));

		jsonInput = new JsonInput();
		jsonInput.setId("Input_json");
		JsonObject obj = new JsonObject();
		jsonInput.setJson(obj);
		JsonDerivedValue derivedValue = new JsonDerivedValue();
		derivedValue.setInput(listInput);
		obj.setItems(new HashMap<>());
		obj.getItems().put("derived", derivedValue);

		annotation.addInput(listInput);
		annotation.addInput(jsonInput);
	}

	@Test
	public void test() {
		String formatted = new InputFormatter().getInputString(jsonInput);

		System.out.println(formatted);
	}

}
