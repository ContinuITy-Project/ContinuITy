package org.continuity.wessbas.transform.jmeter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.wessbas.entities.JMeterTestPlanPack;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Henning Schulz
 *
 */
public class JMeterJsonTest {

	private ObjectMapper mapper = new ObjectMapper(new JsonFactory());

	private JMeterTestPlanPack testPlanPack;

	private String referenceJson;

	@Before
	public void setup() throws IOException {
		mapper.registerModule(new SimpleModule().addDeserializer(ListedHashTree.class, new JMeterTestPlanDeserializer()));

		WessbasToJmeterConverter converter = new WessbasToJmeterConverter("configuration");
		ListedHashTree testPlan = converter.convertToLoadTest(WessbasDslInstance.DVDSTORE_PARSED.get()).getTestPlan();
		testPlanPack = new JMeterTestPlanPack(testPlan, Collections.singletonMap("mybeh", new String[][] {}));
		testPlanPack.setTag("annotation/link");

		StringBuilder builder = new StringBuilder();
		builder.append("{\"behaviors\":{\"mybeh\":[]},");
		builder.append("\"test-plan\":\"");
		builder.append(toJmxString(testPlan));
		builder.append("\",");
		builder.append("\"annotation-link\":\"annotation/link\"}");

		referenceJson = builder.toString();
	}

	@Test
	public void testJson() throws IOException {
		String json = mapper.writeValueAsString(testPlanPack);
		Assert.assertEquals("Serialized test plan pack should be equal to the reference!", referenceJson, json);

		JMeterTestPlanPack deserialized = mapper.readValue(json, JMeterTestPlanPack.class);
		String origJmx = toJmxString(testPlanPack.getTestPlan());
		String readJmx = toJmxString(deserialized.getTestPlan());
		Assert.assertEquals("Re-deserialized test plan should be equal to the original one!", origJmx, readJmx);
	}

	private String toJmxString(ListedHashTree testPlan) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SaveService.saveTree(testPlan, out);
		return new String(out.toByteArray(), "UTF-8").replaceAll("\\\\", "\\\\\\\\").replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n").replaceAll("\\\"", "\\\\\"");
	}

}
