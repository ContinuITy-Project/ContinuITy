package org.continuity.cli.entities;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * @author Henning Schulz
 *
 */
public class TestPlanDeserializer extends StdDeserializer<ListedHashTree> {

	/**
	 *
	 */
	private static final long serialVersionUID = 7055058660506223948L;

	/**
	 *
	 */
	public TestPlanDeserializer() {
		this(null);
	}

	/**
	 * @param vc
	 */
	protected TestPlanDeserializer(Class<?> vc) {
		super(vc);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListedHashTree deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String jmx = p.readValueAs(String.class);
		ByteArrayInputStream input = new ByteArrayInputStream(jmx.getBytes("UTF-8"));
		@SuppressWarnings("deprecation")
		HashTree deserialized = SaveService.loadTree(input);
		return (ListedHashTree) deserialized;
	}

}
