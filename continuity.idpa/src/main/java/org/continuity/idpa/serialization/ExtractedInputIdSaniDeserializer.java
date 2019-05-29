package org.continuity.idpa.serialization;

import java.io.IOException;
import java.util.List;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.extracted.AbstractValueExtraction;
import org.continuity.idpa.annotation.extracted.EndpointOrInput;
import org.continuity.idpa.annotation.extracted.ExtractedInput;
import org.continuity.idpa.annotation.json.JsonItem;
import org.continuity.idpa.visitor.FindBy;

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
 * To be used for fixing {@link EndpointOrInput}s, e.g., in {@link AbstractValueExtraction}. It will
 * set the {@code input} field based on {@code rawInputId}.
 *
 * @author Henning Schulz
 *
 */
public class ExtractedInputIdSaniDeserializer extends StdDeserializer<ApplicationAnnotation> implements ContextualDeserializer, ResolvableDeserializer {

	private static final long serialVersionUID = -1447929075458425682L;

	private final JsonDeserializer<ApplicationAnnotation> delegate;

	public ExtractedInputIdSaniDeserializer(JsonDeserializer<ApplicationAnnotation> delegate) {
		super(JsonItem.class);

		this.delegate = delegate;
	}

	@Override
	public ApplicationAnnotation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ApplicationAnnotation ann = delegate.deserialize(p, ctxt);

		ann.getInputs().stream().filter(ExtractedInput.class::isInstance).map(ExtractedInput.class::cast).map(ExtractedInput::getExtractions).flatMap(List::stream)
				.map(org.continuity.idpa.annotation.extracted.ValueExtraction::getFrom).filter(EndpointOrInput::isInput).forEach(eoi -> {
					Input foundInput = FindBy.findById(eoi.getRawInputId(), Input.class).in(ann).getFound();
					eoi.setRawInputId(null);
					eoi.setInput(foundInput);
				});

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
			return new ExtractedInputIdSaniDeserializer((JsonDeserializer<ApplicationAnnotation>) contextual);
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

}
