package org.continuity.idpa.serialization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.json.JsonDerivedValue;
import org.continuity.idpa.annotation.json.JsonInput;
import org.continuity.idpa.annotation.json.JsonItem;
import org.continuity.idpa.annotation.json.JsonStaticValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Resolves the references of {@link JsonDerivedValue}s to other {@link Input}s. Note that this is
 * done on the string basis, i.e., if the string value is present as an ID, it will be mapped to an
 * input.
 *
 * @author Henning Schulz
 *
 */
public class JsonItemSaniDeserializer extends StdDeserializer<ApplicationAnnotation> implements ContextualDeserializer, ResolvableDeserializer {

	private static final long serialVersionUID = -8628127309808783257L;

	private final JsonDeserializer<ApplicationAnnotation> delegate;

	public JsonItemSaniDeserializer(JsonDeserializer<ApplicationAnnotation> delegate) {
		super(JsonItem.class);

		this.delegate = delegate;
	}

	@Override
	public ApplicationAnnotation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ApplicationAnnotation ann = delegate.deserialize(p, ctxt);

		ItemReplacer replacer = new ItemReplacer(ann);
		ann.getInputs().stream().filter(i -> i instanceof JsonInput).map(i -> (JsonInput) i).forEach(replacer::replaceItems);

		return ann;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		if (delegate instanceof ContextualDeserializer) {
			JsonDeserializer<?> contextual = ((ContextualDeserializer) delegate).createContextual(ctxt, property);
			return new JsonItemSaniDeserializer((JsonDeserializer<ApplicationAnnotation>) contextual);
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		if (delegate instanceof ResolvableDeserializer) {
			((ResolvableDeserializer) delegate).resolve(ctxt);
		}
	}

	private class ItemReplacer {

		private final Map<String, Input> inputPerId;

		public ItemReplacer(ApplicationAnnotation ann) {
			if (ann.getInputs() != null) {
				this.inputPerId = ann.getInputs().stream().filter(i -> (i != null) && (i.getId() != null)).collect(Collectors.toMap(Input::getId, i -> i));
			} else {
				this.inputPerId = Collections.emptyMap();
			}
		}

		public void replaceItems(JsonInput input) {
			if (!input.isLegacy()) {
				JsonItem newItem = replaceItem(input.getJson());
				input.setJson(newItem);
			}
		}

		private JsonItem replaceItem(JsonItem item) {
			switch (item.getType()) {
			case STATIC_VALUE:
				JsonStaticValue val = item.asStaticValue();
				Input input = inputPerId.get(val.getValue());

				if (input != null) {
					JsonDerivedValue newVal = new JsonDerivedValue();
					newVal.setInput(input);
					return newVal;
				}
				break;
			case ARRAY:
				List<JsonItem> newItems = item.asArray().getItems().stream().map(this::replaceItem).collect(Collectors.toList());
				item.asArray().setItems(newItems);
				break;
			case OBJECT:
				Map<String, JsonItem> newItemMap = item.asObject().getItems().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> replaceItem(e.getValue())));
				item.asObject().setItems(newItemMap);
				break;
			case DERIVED_VALUE:
			default:
				break;

			}

			return item;
		}
	}

}
