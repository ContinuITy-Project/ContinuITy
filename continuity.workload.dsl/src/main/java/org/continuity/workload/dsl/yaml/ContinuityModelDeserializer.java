package org.continuity.workload.dsl.yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.continuity.workload.dsl.ContinuityModelElement;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;

/**
 * @author Henning Schulz
 *
 */
public class ContinuityModelDeserializer extends JsonDeserializer<ContinuityModelElement> implements ContextualDeserializer, ResolvableDeserializer {

	private final JsonDeserializer<Object> defaultDeserializer;

	/**
	 *
	 */
	public ContinuityModelDeserializer(JsonDeserializer<Object> defaultDeserializer) {
		this.defaultDeserializer = defaultDeserializer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ContinuityModelElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		IdHandlingGeneratorDelegate delegate = new IdHandlingGeneratorDelegate(p);
		ContinuityModelElement deserialized = (ContinuityModelElement) defaultDeserializer.deserialize(delegate, ctxt);

		for (Entry<String, String> entry : delegate.getExtractedFields().entrySet()) {
			WeakReferenceResolver resolver = WeakReferenceResolver.get(deserialized.getClass());

			if (resolver.isWeakReference(entry.getKey())) {
				resolver.resolveWeakReference(entry.getKey(), entry.getValue(), deserialized);
			}
		}

		return deserialized;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		if (defaultDeserializer instanceof ContextualDeserializer) {
			JsonDeserializer<?> contextual = ((ContextualDeserializer) defaultDeserializer).createContextual(ctxt, property);
			return new ContinuityModelDeserializer((JsonDeserializer<Object>) contextual);
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		if (defaultDeserializer instanceof ResolvableDeserializer) {
			((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
		}
	}

	private static class IdHandlingGeneratorDelegate extends JsonParserDelegate {

		private final Map<String, String> extractedFields = new HashMap<>();

		public IdHandlingGeneratorDelegate(JsonParser d) {
			super(d);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getText() throws IOException {
			String text = super.getText();
			extractedFields.put(getCurrentName(), text);
			return text;
		}

		/**
		 * Gets {@link #extractedFields}.
		 *
		 * @return {@link #extractedFields}
		 */
		public Map<String, String> getExtractedFields() {
			return this.extractedFields;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getTypeId() throws IOException {
			System.out.println(getCurrentName() + ": " + (getCurrentValue() == null ? "null" : getCurrentValue().getClass()));
			return super.getTypeId();
		}

	}

}
