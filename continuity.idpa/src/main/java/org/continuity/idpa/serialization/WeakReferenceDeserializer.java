package org.continuity.idpa.serialization;

import java.io.IOException;

import org.continuity.idpa.WeakReference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

/**
 * @author Henning Schulz
 *
 */
public class WeakReferenceDeserializer extends StdDeserializer<WeakReference<?>> {

	/**
	 *
	 */
	private static final long serialVersionUID = -8041901656599411461L;

	/**
	 *
	 */
	public WeakReferenceDeserializer() {
		this(null);
	}

	protected WeakReferenceDeserializer(Class<?> vc) {
		super(vc);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WeakReference<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String deserialized = StringDeserializer.instance.deserialize(p, ctxt);
		return WeakReference.createUntyped(deserialized);
	}

}
