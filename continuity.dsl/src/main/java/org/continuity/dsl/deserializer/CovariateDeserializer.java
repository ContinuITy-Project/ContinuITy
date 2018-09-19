package org.continuity.dsl.deserializer;

import java.io.IOException;

import org.continuity.dsl.description.FutureNumbers;
import org.continuity.dsl.description.ContextParameter;
import org.continuity.dsl.description.FutureEvents;
import org.continuity.dsl.description.Measurement;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Custom deserializer to differentiate between different types of covariate.
 * values.
 * 
 * @author Alper Hidiroglu
 *
 */
public class CovariateDeserializer extends JsonDeserializer<ContextParameter> {

	@Override
	public ContextParameter deserialize(JsonParser p, DeserializationContext cntxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		ObjectNode root = mapper.readTree(p);

		ContextParameter covariate = null;

		if (!root.has("future")) {
			covariate = mapper.readValue(root.toString(), Measurement.class);
		} else if (root.get("future").findValue("value").isTextual()) {
			covariate = mapper.readValue(root.toString(), FutureEvents.class);
		} else if (root.get("future").findValue("value").isNumber()) {
			covariate = mapper.readValue(root.toString(), FutureNumbers.class);
		} else {
			throw new IOException("Invalid context input!");
		}
		return covariate;
	}
}