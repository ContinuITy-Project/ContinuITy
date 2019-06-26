package org.continuity.idpa.serialization;

import org.continuity.idpa.annotation.extracted.JsonPathExtraction;
import org.continuity.idpa.annotation.extracted.RegExExtraction;
import org.continuity.idpa.annotation.extracted.ValueExtraction;

public class ValueExtractionDeserializer extends UniquePropertyPolymorphicDeserializer<ValueExtraction> {

	private static final long serialVersionUID = 7758011854273993656L;

	public ValueExtractionDeserializer() {
		super(ValueExtraction.class);

		register("pattern", RegExExtraction.class);
		register("json-path", JsonPathExtraction.class);
	}

}
