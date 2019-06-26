package org.continuity.forecast.context.deserializer;

import java.io.IOException;

import org.continuity.forecast.context.BooleanCovariateValue;
import org.continuity.forecast.context.CovariateValue;
import org.continuity.forecast.context.NumericalCovariateValue;
import org.continuity.forecast.context.StringCovariateValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Custom deserializer to differentiate between different types of covariate
 * values.
 * 
 * @author Alper Hidiroglu
 *
 */
public class CovariateDeserializer extends JsonDeserializer<CovariateValue> {

	@Override
	public CovariateValue deserialize(JsonParser p, DeserializationContext cntxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		ObjectNode root = mapper.readTree(p);

		CovariateValue covarVal = null;

		if (root.has("value")) {
			if (root.get("value").isTextual()) {
				covarVal = mapper.readValue(root.toString(), StringCovariateValue.class);
			} else if (root.get("value").isBoolean()) {
				covarVal = mapper.readValue(root.toString(), BooleanCovariateValue.class);
			} else if (root.get("value").isNumber()) {
				covarVal = mapper.readValue(root.toString(), NumericalCovariateValue.class);
			}
		}
		return covarVal;
	}
}