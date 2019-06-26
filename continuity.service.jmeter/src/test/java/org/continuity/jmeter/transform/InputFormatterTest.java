package org.continuity.jmeter.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CombinedInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.RandomNumberInput;
import org.continuity.idpa.annotation.RandomStringInput;
import org.continuity.idpa.annotation.json.JsonDerivedValue;
import org.continuity.idpa.annotation.json.JsonInput;
import org.continuity.idpa.annotation.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InputFormatterTest {

	private static final String EXPECTED_JSON = "{\"derived\":\"42\"}";

	private static final String EXPECTED_RANDOM_NUMBER = "${__Random(5,42,)}";

	private static final String EXPECTED_RANDOM_STRING = "${__RandomString(8,0123456789ABCD,)}-${__RandomString(4,0123456789ABCD,)}-${__RandomString(4,0123456789ABCD,)}-${__RandomString(4,0123456789ABCD,)}-${__RandomString(12,0123456789ABCD,)}";

	private static final String EXPECTED_COMBINED = EXPECTED_RANDOM_NUMBER + "-42: " + EXPECTED_RANDOM_STRING;

	private ApplicationAnnotation annotation;

	private JsonInput jsonInput;

	private RandomNumberInput randomNumberInput;

	private RandomStringInput randomStringInput;

	private CombinedInput combinedInput;

	@Before
	public void setup() {
		this.annotation = new ApplicationAnnotation();

		DirectListInput listInput = new DirectListInput();
		listInput.setId("Input_list");
		listInput.setData(Collections.singletonList("42"));

		jsonInput = new JsonInput();
		jsonInput.setId("Input_json");
		JsonObject obj = new JsonObject();
		jsonInput.setJson(obj);
		JsonDerivedValue derivedValue = new JsonDerivedValue();
		derivedValue.setInput(listInput);
		obj.setItems(new HashMap<>());
		obj.getItems().put("derived", derivedValue);

		randomNumberInput = new RandomNumberInput();
		randomNumberInput.setId("Input_random_number");
		randomNumberInput.setStaticLowerLimit(5);
		randomNumberInput.setDerivedUpperLimit(listInput);

		randomStringInput = new RandomStringInput();
		randomStringInput.setId("Input_random_string");
		randomStringInput.setTemplate("[0-9A-D]{8}\\-[0-9A-D]{4}\\-[0-9A-D]{4}\\-[0-9A-D]{4}\\-[0-9A-D]{12}");

		combinedInput = new CombinedInput();
		combinedInput.setId("Input_combined");
		combinedInput.setFormat("(1)-(2): (3)");
		combinedInput.setInputs(new ArrayList<>());
		combinedInput.getInputs().add(randomNumberInput);
		combinedInput.getInputs().add(listInput);
		combinedInput.getInputs().add(randomStringInput);

		annotation.addInput(listInput);
		annotation.addInput(jsonInput);
		annotation.addInput(randomNumberInput);
		annotation.addInput(randomStringInput);
	}

	@Test
	public void testJsonInput() {
		test(jsonInput, EXPECTED_JSON);
	}

	@Test
	public void testRandomNumberInput() {
		test(randomNumberInput, EXPECTED_RANDOM_NUMBER);
	}

	@Test
	public void testRandomStringInput() {
		test(randomStringInput, EXPECTED_RANDOM_STRING);
	}

	@Test
	public void testCombinedInput() {
		test(combinedInput, EXPECTED_COMBINED);
	}

	@Test
	public void testBrokenCombinedInput() {
		combinedInput.setFormat(combinedInput.getFormat() + " (4)");

		test(combinedInput, EXPECTED_COMBINED + " (4)");
	}

	private void test(Input input, String expected) {
		String formatted = new InputFormatter().getInputString(input);

		Assert.assertEquals(expected, formatted);
	}

}
