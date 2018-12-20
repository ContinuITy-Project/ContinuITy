package org.continuity.idpa.serialization.yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.continuity.idpa.IdpaElement;
import org.continuity.idpa.annotation.Input;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
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
public class IdpaDeserializer extends JsonDeserializer<IdpaElement> implements ContextualDeserializer, ResolvableDeserializer {

	private final JsonDeserializer<Object> defaultDeserializer;

	private final Map<Object, Object> typeIds = new HashMap<>();

	/**
	 *
	 */
	public IdpaDeserializer(JsonDeserializer<Object> defaultDeserializer) {
		this.defaultDeserializer = defaultDeserializer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IdpaElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		TypeHandlingGeneratorDelegate delegate = new TypeHandlingGeneratorDelegate(p, this);
		IdpaElement deserialized = (IdpaElement) defaultDeserializer.deserialize(delegate, ctxt);

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
			return new IdpaDeserializer((JsonDeserializer<Object>) contextual);
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

	private static class TypeHandlingGeneratorDelegate extends JsonParserDelegate {

		private static final Class<?>[] superTypes = { Input.class };

		private final IdpaDeserializer deser;

		public TypeHandlingGeneratorDelegate(JsonParser d, IdpaDeserializer deser) {
			super(d);
			this.deser = deser;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getObjectId() throws IOException {
			Object id = super.getObjectId();

			if (id != null) {
				deser.typeIds.put(id, getTypeIdOfObject(super.getCurrentValue()));
			}

			return id;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getTypeId() throws IOException {
			Object id = super.getTypeId();

			if (id != null) {
				return id;
			} else {
				Object refId = super.getText();

				if (refId != null) {
					return deser.typeIds.get(refId);
				}
			}

			return null;
		}

		private Object getTypeIdOfObject(Object object) {
			if (object == null) {
				return null;
			}

			for (Class<?> clazz : superTypes) {
				JsonSubTypes types = clazz.getAnnotation(JsonSubTypes.class);

				if (types != null) {
					for (Type type : types.value()) {
						if (type.value().equals(object.getClass())) {
							return type.name();
						}
					}
				}
			}

			return null;
		}

	}

}
